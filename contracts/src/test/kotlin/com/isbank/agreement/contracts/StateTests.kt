package com.isbank.agreement.contracts

import com.isbank.agreement.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.testing.node.MockServices
import org.junit.Test

class StateTests {
    private val ledgerServices = MockServices()
    @Test
    @Throws(NoSuchFieldException::class)
    fun hasAmountFieldOfCorrectType() {
        // Does the message field exist?
        //IOUState::class.java.getDeclaredField("amount")
        //assert(IOUState::class.java.getDeclaredField("amount").getType() == Amount::class)
    }
}