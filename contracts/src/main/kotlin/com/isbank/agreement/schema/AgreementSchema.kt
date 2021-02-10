package com.isbank.agreement.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table
//4.6 changes
import org.hibernate.annotations.Type


/**
 * The family of schemas for IOUState.
 */
object AgreementSchema

/**
 * An IOUState schema.
 */
object AgreementSchemaV1 : MappedSchema(
        schemaFamily = AgreementSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentAgreement::class.java)) {

    override val migrationResource: String?
        get() = "agreement.changelog-master";

    @Entity
    @Table(name = "agreement_states")
    class PersistentAgreement(
            @Column(name = "linear_id")
            @Type(type = "uuid-char")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this(UUID.randomUUID())
    }
}