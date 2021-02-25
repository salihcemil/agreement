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
import com.isbank.agreement.states.AgreementState
import com.isbank.agreement.contracts.AgreementContract
import com.isbank.agreement.states.Status
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the Agreement encapsulated
 * within an [AgreementState].
 *
 * In our simple example, the [Acceptor] always accepts a valid Agreement.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the varAgreements stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object RejectAgreementFlow {
    @InitiatingFlow
    @StartableByRPC
    @StartableByService
    class Initiator(val agreementStateID: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new Agreement.")
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

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(agreementStateID))
            val results = serviceHub.vaultService.queryBy<AgreementState>(criteria)
            val inputAgreementState = results.states.first()
            val outputAgreementState = inputAgreementState.state.data.withNewStatus(Status.REJECTED)

            // Obtain a reference from a notary we wish to use.
            val notary = NotaryUtils.getNotary(serviceHub)

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val txCommand = Command(AgreementContract.Commands.Expire(), listOf(inputAgreementState.state.data.acquirer.owningKey, inputAgreementState.state.data.issuer.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(inputAgreementState)
                    .addOutputState(outputAgreementState, AgreementContract.ID)
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
            val otherSessions = listOf(inputAgreementState.state.data.issuer, inputAgreementState.state.data.acquirer)
            val sessions = (otherSessions - ourIdentity).map { initiateFlow(it) }.toSet()
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, sessions, GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, sessions, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Agreement transaction." using (output is AgreementState)
                    val Agreement = output as AgreementState
                    "Issuer is not Acquirer" using (Agreement.issuer != Agreement.acquirer)
                    "PAN is invalid" using (Agreement.pan.isNotEmpty()) // TODO: Define valid PAN
               /*     "Amount is greater then " using (Agreement.) // TODO: Define valid amount  */
                    "The AgreementState should be set to REJECTED." using (Agreement.status == Status.REJECTED)
                }
            }
            val txId = subFlow(signTransactionFlow).id
            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}
