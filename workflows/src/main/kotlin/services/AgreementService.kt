package services

import com.isbank.agreement.flows.*
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

    fun DecideAgreements() {
        val threadId = Thread.currentThread().id
        loggerFor<AgreementService>().info("Running test on thread($threadId).")
 /*  <----- Accept or reject   start ----- */
        val generalCriteria2 = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results2 = builder {
            // Compare validUntil field versus current time
          /*  val validUntilIndex2 = AgreementSchemaV1.PersistentAgreement::t_validUntil.greaterThan(Date())
            val queryCriteriaValidUntil2 = QueryCriteria.VaultCustomQueryCriteria(validUntilIndex2)
           * val queryCriteria2 = generalCriteria2*.and(queryCriteriaValidUntil2)*/
            // SQL statement matching queryCriteria = "SELECT * FROM agreement_states WHERE Status='UNCONSUMED' and t_validUntil > NOW()"
            val pageSpecification2 = PageSpecification()
     //       val myAgreements2 = serviceHub.vaultService.queryBy<AgreementState>(AgreementState::class.java, criteria = queryCriteria2, paging = pageSpecification2).states
            val myAgreements2 = serviceHub.vaultService.queryBy<AgreementState>(AgreementState::class.java, criteria = generalCriteria2, paging = pageSpecification2).states

            // TODO: if already executing flow, don't start a new one until finished
            myAgreements2.map{
                //expire
                if(it.state.data.validUntil < Date()) {
                    serviceHub.startFlow((ExpireAgreementFlow::Initiator)(it.state.data.linearId)).returnValue.get()
                    serviceHub.startFlow((ConsumeAgreementFlow::Initiator)(it.state.data.linearId)).returnValue.get()
                }
                //reject
                else if(it.state.data.amount.quantity !in 0..50000000) {
                    serviceHub.startFlow((RejectAgreementFlow::Initiator)(it.state.data.linearId)).returnValue.get()
                    serviceHub.startFlow((ConsumeAgreementFlow::Initiator)(it.state.data.linearId)).returnValue.get()
                }
                //accept
                else {
                    serviceHub.startFlow((AcceptAgreementFlow::Initiator)(it.state.data.linearId)).returnValue.get()
                    serviceHub.startFlow((ConsumeAgreementFlow::Initiator)(it.state.data.linearId)).returnValue.get()
                }
            }
        }


    loggerFor<AgreementService>().info("Builder  accept reject result2($results2).")
 /* end Accept or reject  -----> */
    }

    private fun processEvent(event: ServiceLifecycleEvent) {
        // Lifecycle event handling code including full use of serviceHub
        when (event) {
            ServiceLifecycleEvent.STATE_MACHINE_STARTED -> {
                val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
                scheduledExecutorService.scheduleAtFixedRate({ DecideAgreements() }, 2, 5, TimeUnit.SECONDS)
            }
            else -> {
                // Process other types of events

            }
        }
    }

    // public api of service
}