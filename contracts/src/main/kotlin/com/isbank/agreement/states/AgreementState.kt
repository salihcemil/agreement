package com.isbank.agreement.states

import com.isbank.agreement.contracts.AgreementContract
import com.isbank.agreement.schema.AgreementSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

/**
 * The state object recording  agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *

 * @param issuer the party issuing the agreement.
 * @param acquirer the party receiving and approving .
 */
@BelongsToContract(AgreementContract::class)
data class AgreementState(val issuer: Party,
                          val acquirer: Party,
                          val pan : String,
                          val timeAndDate: Date,
                          val validUntil : Date,
                          val amount: Amount<Currency>,
                          override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(issuer, acquirer)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is AgreementSchemaV1 -> AgreementSchemaV1.PersistentAgreement(
                this.issuer.name.toString(),
                this.acquirer.name.toString(),
                this.pan,
                this.timeAndDate,
                this.validUntil,
                this.amount.quantity,
                this.amount.token.currencyCode,
                this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(AgreementSchemaV1)
}
