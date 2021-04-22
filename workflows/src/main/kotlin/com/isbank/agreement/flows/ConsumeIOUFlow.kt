package com.isbank.agreement.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import com.isbank.agreement.contracts.IOUContract
import com.isbank.agreement.states.IOUState
import com.isbank.agreement.states.IOUState.Status
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import java.util.*
import java.util.concurrent.TimeUnit

object ConsumeIOUFlow {
    @InitiatingFlow
    @StartableByRPC
    @StartableByService
    class Initiator(val IOUStateID: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            // Step 1. Retrieve the IOU state from the vault.
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(IOUStateID))
            val iouToSettle = serviceHub.vaultService.queryBy<IOUState>(queryCriteria).states.single()

            if (ourIdentity != iouToSettle.state.data.acquirer) {
                throw IllegalArgumentException("IOU settlement flow must be initiated by the acquirer.")
            }

            // Obtain a reference from a notary we wish to use.
            val notary = NotaryUtils.getNotary(serviceHub)

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val txCommand = Command(IOUContract.Commands.Consume(), listOf(iouToSettle.state.data.acquirer.owningKey, iouToSettle.state.data.issuer.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(iouToSettle)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION

            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val otherSessions = listOf(iouToSettle.state.data.issuer, iouToSettle.state.data.acquirer)
            val sessions = (otherSessions - ourIdentity).map { initiateFlow(it) }.toSet()
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, sessions, GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, sessions, FINALISING_TRANSACTION.childProgressTracker()))
        }

        @InitiatedBy(ConsumeIOUFlow.Initiator::class)
        class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
            @Suspendable
            override fun call(): SignedTransaction {
                val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                    override fun checkTransaction(stx: SignedTransaction) = requireThat {
                        "There must be no outputs." using (stx.tx.outputs.isEmpty())
                    }
                }
                val txId = subFlow(signTransactionFlow).id
                return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
            }
    }
}}