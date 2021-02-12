package com.isbank.agreement.dao

import net.corda.core.schemas.MappedSchema
import net.corda.core.serialization.CordaSerializable
import org.hibernate.annotations.Type
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

object AgreementDAOSchema

object AgreementDAOSchemaV1 : MappedSchema(
        schemaFamily = AgreementDAOSchema.javaClass,
        version = 1,
        mappedTypes = listOf(AgreementDAO::class.java)
) {
    override val migrationResource: String?
        get() = "agreementdao.changelog-master";

    @Entity
    @Table(name = "agreement_daos")
    @CordaSerializable
    // TODO: change to use serialization whitelist: https://docs.corda.net/docs/corda-os/4.7/serialization.html#whitelisting
    class AgreementDAO(
            @Id
            @Column(name = "agreement_state_id")
            @Type(type = "uuid-char")
            var agreementStateLinearID: UUID,
            @Column(name = "pan")
            var pan: String
    ) : Serializable
}