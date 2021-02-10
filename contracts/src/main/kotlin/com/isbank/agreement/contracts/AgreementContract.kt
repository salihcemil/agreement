package com.isbank.agreement.contracts

import com.isbank.agreement.states.AgreementState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

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
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
            // Generic constraints around the IOU transaction.
            "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<AgreementState>().single()
            "The issuer and the acquirer cannot be the same entity." using (out.issuer != out.acquirer)
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

            // IOU-specific constraints.
            //"The IOU's value must be non-negative." using (out.value > 0) //salihcemil
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
    }
}
