package services

import com.isbank.agreement.flows.ConsumeAgreementFlow
import com.isbank.agreement.flows.ExpireAgreementFlow
import com.isbank.agreement.schema.AgreementSchemaV1
import com.isbank.agreement.states.AgreementState
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.ServiceLifecycleEvent
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@CordaService
class AgreementService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    init {
        // Custom code ran at service creation

        // Optional: Express interest in receiving lifecycle events
        serviceHub.register { processEvent(it) }
    }

    fun ExpireAgreements() {
        val threadId = Thread.currentThread().id
        loggerFor<AgreementService>().info("Running test on thread($threadId).")

        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)

        val results = builder {
            // Compare validUntil field versus current time
            val validUntilIndex = AgreementSchemaV1.PersistentAgreement::t_validUntil.lessThan(Date())
            val queryCriteriaValidUntil = QueryCriteria.VaultCustomQueryCriteria(validUntilIndex)
            val queryCriteria = generalCriteria.and(queryCriteriaValidUntil)
            // SQL statement matching queryCriteria = "SELECT * FROM agreement_states WHERE Status='UNCONSUMED' and t_validUntil < NOW()"
            val pageSpecification = PageSpecification()
            val myAgreements = serviceHub.vaultService.queryBy<AgreementState>(AgreementState::class.java, criteria = queryCriteria, paging = pageSpecification).states

            // TODO: if already executing flow, don't start a new one until finished
            myAgreements.map {
                serviceHub.startFlow((ExpireAgreementFlow::Initiator)(it.state.data.linearId)).returnValue.get()
                serviceHub.startFlow((ConsumeAgreementFlow::Initiator)(it.state.data.linearId)).returnValue.get()
            }
        }
    }

    private fun processEvent(event: ServiceLifecycleEvent) {
        // Lifecycle event handling code including full use of serviceHub
        when (event) {
            ServiceLifecycleEvent.STATE_MACHINE_STARTED -> {
                val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
                scheduledExecutorService.scheduleAtFixedRate({ ExpireAgreements() }, 10, 10, TimeUnit.SECONDS)
            }
            else -> {
                // Process other types of events
            }
        }
    }

    // public api of service
}