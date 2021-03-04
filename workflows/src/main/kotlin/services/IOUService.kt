//package services
//
//import com.isbank.agreement.flows.*
//import com.isbank.agreement.states.AgreementState
//
//import net.corda.core.node.AppServiceHub
//import net.corda.core.node.services.CordaService
//import net.corda.core.node.services.ServiceLifecycleEvent
//import net.corda.core.node.services.Vault
//import net.corda.core.node.services.vault.PageSpecification
//import net.corda.core.node.services.vault.QueryCriteria
//import net.corda.core.node.services.vault.builder
//import net.corda.core.serialization.SingletonSerializeAsToken
//import net.corda.core.utilities.loggerFor
//import java.util.concurrent.Executors
//import java.util.concurrent.TimeUnit
//
//@CordaService
//class IOUService(private val serviceHub: AppServiceHub): SingletonSerializeAsToken() {
//    init {
//        // Custom code ran at service creation
//
//        // Optional: Express interest in receiving lifecycle events
//        serviceHub.register { processEvent(it) }
//    }
//
//    fun createIOUFromAcceptedAgreements(){
//        val threadId = Thread.currentThread().id
//        loggerFor<AgreementService>().info("Running test on thread($threadId).")
//        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)
//
//        val results = builder{
//            val pageSpecification2 = PageSpecification()
//            val consumedAgreements = serviceHub.vaultService.queryBy<AgreementState>(AgreementState::class.java, criteria = generalCriteria, paging = pageSpecification2).states
//
//            consumedAgreements.map{
//                //serviceHub.startFlow()
//                serviceHub.startFlow((CreateIOUFromAgreement::Initiator)(it.state.data.amount.quantity,
//                                                            it.state.data.acquirer,
//                                                            it.state.data.timeAndDate,
//                                                            it.state.data.amount.token.currencyCode)).returnValue.get()
//            }
//        }
//
//        loggerFor<AgreementService>().info("Builder  accept reject result($results).")
//        /* end Accept or reject  -----> */
//    }
//
//
//    private fun processEvent(event: ServiceLifecycleEvent) {
//        // Lifecycle event handling code including full use of serviceHub
//        when (event) {
//            ServiceLifecycleEvent.STATE_MACHINE_STARTED -> {
//                val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
//                scheduledExecutorService.scheduleAtFixedRate({ createIOUFromAcceptedAgreements() }, 2, 5, TimeUnit.SECONDS)
//            }
//            else -> {
//                // Process other types of events
//
//            }
//        }
//    }
//}