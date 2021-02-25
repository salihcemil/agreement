package com.isbank.agreement.flows

import co.paralleluniverse.fibers.Suspendable
import com.isbank.agreement.contracts.AgreementContract
import com.isbank.agreement.dao.AgreementDAOSchemaV1
import com.isbank.agreement.flows.CreateAgreementFlow.Acceptor
import com.isbank.agreement.flows.CreateAgreementFlow.Initiator
import com.isbank.agreement.states.AgreementState
import com.isbank.agreement.states.Status
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap
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
object CreateAgreementFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val issuer: Party,
                    val pan : String,
                    val amount: Amount<Currency>) : FlowLogic<SignedTransaction>() {
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
            val timeAndDate = Date(System.currentTimeMillis())
            val validUntil = Date(timeAndDate.time + TimeUnit.MINUTES.toMillis(1)) //TODO: Set to 5 minutes

            // Obtain a reference from a notary we wish to use.
            val notary = NotaryUtils.getNotary(serviceHub)

            val linearId: UniqueIdentifier = UniqueIdentifier()
            val agreementDAO = AgreementDAOSchemaV1.AgreementDAO(linearId.id, pan)
            serviceHub.withEntityManager {
                persist(agreementDAO)
            }

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val AgreementState = AgreementState(Status.PROPOSED, issuer, serviceHub.myInfo.legalIdentities.first(), pan, timeAndDate, validUntil, amount, linearId)
            val txCommand = Command(AgreementContract.Commands.Propose(), listOf(serviceHub.myInfo.legalIdentities.first().owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(AgreementState, AgreementContract.ID)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val fullySignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val otherPartySession = initiateFlow(issuer)
            otherPartySession.send(agreementDAO)
            //val fullySignedTx = subFlow(CollectSignat  uresFlow(partSignedTx, setOf(otherPartySession), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        var txId : SignedTransaction? = null

        @Suspendable
        override fun call(): SignedTransaction {
            val agreementDAOPacket: UntrustworthyData<AgreementDAOSchemaV1.AgreementDAO> = otherPartySession.receive<AgreementDAOSchemaV1.AgreementDAO>()
            val agreementDAO: AgreementDAOSchemaV1.AgreementDAO = agreementDAOPacket.unwrap { data ->
                // Perform checking on the object received.
                // TODO: Check the received object.
                // Return the object.
                data
            }
            serviceHub.withEntityManager {
                persist(agreementDAO)
            }

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId?.id))
        }
    }
}
