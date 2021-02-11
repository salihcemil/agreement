package com.isbank.agreement.contracts

import net.corda.core.identity.CordaX500Name
import com.isbank.agreement.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class ContractTests {
    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))
    private val iouValue = 1

    @Test
    fun `transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState( miniCorp.party, megaCorp.party, Date(), UUID.randomUUID(), Amount(0, Currency.getInstance("USD"))))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(IOUContract.ID, IOUState( miniCorp.party, megaCorp.party, Date(), UUID.randomUUID(), Amount(0, Currency.getInstance("USD"))))
                output(IOUContract.ID, IOUState( miniCorp.party, megaCorp.party, Date(), UUID.randomUUID(), Amount(0, Currency.getInstance("USD"))))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("No inputs should be consumed when issuing an IOU.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState( miniCorp.party, megaCorp.party, Date(), UUID.randomUUID(), Amount(0, Currency.getInstance("USD"))))
                output(IOUContract.ID, IOUState( miniCorp.party, megaCorp.party, Date(), UUID.randomUUID(), Amount(0, Currency.getInstance("USD"))))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `lender must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState( miniCorp.party, megaCorp.party, Date(), UUID.randomUUID(), Amount(0, Currency.getInstance("USD"))))
                command(miniCorp.publicKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `borrower must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState( miniCorp.party, megaCorp.party, Date(), UUID.randomUUID(), Amount(0, Currency.getInstance("USD"))))
                command(megaCorp.publicKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `lender is not borrower`() {
        ledgerServices.ledger {
            transaction {
                output(IOUContract.ID, IOUState( megaCorp.party, megaCorp.party, Date(), UUID.randomUUID(), Amount(0, Currency.getInstance("USD"))))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("The issuer and the acquirer cannot be the same entity.")
            }
        }
    }
}