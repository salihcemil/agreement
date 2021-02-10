package com.isbank.agreement.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.AllAccounts
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

/// For all Accounts SDK flows, see:
/// https://github.com/corda/accounts/tree/master/workflows/src/main/kotlin/com/r3/corda/lib/accounts/workflows/flows
abstract class AccountService {
    @InitiatingFlow
    @StartableByRPC
    class FetchAllAccounts : FlowLogic<List<StateAndRef<AccountInfo>>>() {
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): List<StateAndRef<AccountInfo>> {
            return subFlow(AllAccounts())
        }
    }

    @InitiatingFlow
    @StartableByRPC
    class CreateNewAccount(name: String) : FlowLogic<StateAndRef<AccountInfo>>() {
        private var AccountName = ""

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): StateAndRef<AccountInfo> {
            return subFlow(CreateAccount(AccountName))
        }

        init {
            AccountName = name
        }
    }
}