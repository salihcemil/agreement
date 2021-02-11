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
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the value of the IOU.
 * @param issuer the party issuing the IOU.
 * @param acquirer the party receiving and approving the IOU.
 */
@BelongsToContract(AgreementContract::class)
data class AgreementState(val issuer: Party,
                          val acquirer: Party,
                          val pan : String,
        //val merchant: Account?,
                          val timeAndDate: Date,
                          val validUntil : Date,
                          val agreementStateID: UUID,
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
                this.agreementStateID,
                this.amount.toDecimal().toInt(),
                this.amount.toString(),
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(AgreementSchemaV1)
}
