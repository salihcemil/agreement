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
import net.corda.core.contracts.StateAndRef
//import services.AgreementService
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * This flow fetches all [AgreementState]'s.
 */
object FetchAgreementsFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator() : FlowLogic<List<StateAndRef<AgreementState>>>() {
        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): List<StateAndRef<AgreementState>> {
            val myagreements = serviceHub.vaultService.queryBy<AgreementState>(contractStateType = AgreementState::class.java).states
            return myagreements
        }
    }
}
