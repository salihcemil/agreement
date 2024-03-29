package com.isbank.agreement.states

import com.isbank.agreement.contracts.IOUContract
import com.isbank.agreement.schema.IOUSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the value of the IOU.
 * @param issuer the party issuing the IOU.
 * @param acquirer the party receiving and approving the IOU.
 */
@BelongsToContract(IOUContract::class)
data class IOUState(val issuer: Party,
                          val acquirer: Party,
                          val dueDate: Date,
                          val agreementStateID: UUID,
                          val amount: Amount<Currency>,
                          override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(issuer, acquirer)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is IOUSchemaV1 -> IOUSchemaV1.PersistentIOU(
                    this.issuer.name.toString(),
                    this.acquirer.name.toString(),
                    this.amount.quantity,
                    this.amount.token.currencyCode,
                    this.agreementStateID,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(IOUSchemaV1)
}
