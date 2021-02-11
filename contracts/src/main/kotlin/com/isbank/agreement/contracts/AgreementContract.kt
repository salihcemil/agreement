package com.isbank.agreement.contracts

import com.isbank.agreement.states.AgreementState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.util.*

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [AgreementState], which in turn encapsulates an [AgreementState].
 *
 * For a new [AgreementState] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [AgreementState].
 * - An Create() command with the public keys of both the issuer and the acquirer.
 *
 * All contracts must sub-class the [Contract] interface.
 */
class AgreementContract : Contract {
    companion object {
        @JvmStatic
        val ID = "com.isbank.agreement.contracts.AgreementContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Propose>()
        requireThat {
            "No inputs should be consumed when issuing an Agreement." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)

            val output = tx.outputsOfType<AgreementState>().single()
            "Issuer is not Acquirer" using (output.issuer != output.acquirer)
            "PAN is invalid" using (output.pan.isNotEmpty()) // TODO: Define valid PAN
            "Agreement has expired" using (output.validUntil.after(Date(System.currentTimeMillis())))

            "Acquirer must be signer." using (command.signers.containsAll(listOf(output.acquirer.owningKey)))
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Propose : Commands
        class Accept : Commands
        class Reject : Commands
        class Expire : Commands
        class Consume : Commands
    }
}
