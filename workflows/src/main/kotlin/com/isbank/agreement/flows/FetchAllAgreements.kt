package com.isbank.agreement.flows

import co.paralleluniverse.fibers.Suspendable
import com.isbank.agreement.states.AgreementState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

object FetchAllAgreementsFlow {
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