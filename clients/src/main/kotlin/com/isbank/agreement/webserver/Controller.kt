package com.isbank.agreement.webserver

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.isbank.agreement.flows.AccountService.CreateNewAccount
import com.isbank.agreement.flows.AccountService.FetchAllAccounts
import com.isbank.agreement.flows.ExampleFlow.Initiator
import com.isbank.agreement.states.IOUState
import net.corda.client.jackson.JacksonSupport
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest


val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {


    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    @Bean
    open fun mappingJackson2HttpMessageConverter(@Autowired rpcConnection: NodeRPCConnection): MappingJackson2HttpMessageConverter {
        val mapper = JacksonSupport.createDefaultMapper(rpcConnection.proxy)
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }


    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy

    /**
     * Returns the node's name.
     */
    @GetMapping(value = ["me"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value = ["peers"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Gets all accounts using the Accounts SDK.
     * Example request:
     * curl -X GET 'http://localhost:10050/v1/accounts'
     */
    @GetMapping(value = ["/accounts"], produces = [MediaType.TEXT_PLAIN_VALUE])
    private fun accounts(): ResponseEntity<String?>? {
        var response: ResponseEntity<String?>?
        try {
            val allAccounts: List<StateAndRef<AccountInfo>> = proxy.startFlowDynamic<List<StateAndRef<AccountInfo>>>(FetchAllAccounts::class.java, *arrayOf<Any>()).returnValue.get()
            var output = ""
            for (account in allAccounts) output += account
            response = ResponseEntity.status(HttpStatus.OK).body("AllAccounts: $output")
        } catch (e: Exception) {
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
        return response
    }

    /**
     * Creates an account using the Accounts SDK.
     * Example request:
     * curl -X POST 'http://localhost:10050/v1/accounts/create?accountName=MyNewAccountName'
     */
    @PostMapping(value = ["/accounts/create"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun createAccount(@RequestParam("accountName") accountName: String): ResponseEntity<String?> {
        val response: ResponseEntity<String?>
        response = try {
            proxy.startFlowDynamic<StateAndRef<AccountInfo>>(CreateNewAccount::class.java, *arrayOf<Any>(accountName)).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Account with name: $accountName")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
        return response
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GetMapping(value = ["ious"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getIOUs() : ResponseEntity<List<StateAndRef<IOUState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<IOUState>().states)
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the issuer and the acquirer will be able to
     * see it when calling /spring/api/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PostMapping(value = ["create-iou"], produces = [MediaType.TEXT_PLAIN_VALUE], headers = ["Content-Type=application/x-www-form-urlencoded"])
    fun createIOU(request: HttpServletRequest): ResponseEntity<String> {
        val iouValue = request.getParameter("iouValue").toLong()
        val partyName = request.getParameter("partyName")
        if(partyName == null){
            return ResponseEntity.badRequest().body("Query parameter 'partyName' must not be null.\n")
        }
        if (iouValue <= 0 ) {
            return ResponseEntity.badRequest().body("Query parameter 'iouValue' must be non-negative.\n")
        }
        val partyX500Name = CordaX500Name.parse(partyName)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $partyName cannot be found.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(::Initiator, iouValue, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    /**
     * Displays all IOU states that only this node has been involved in.
     */
    @GetMapping(value = ["my-ious"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getMyIOUs(): ResponseEntity<List<StateAndRef<IOUState>>> {
        val myious = proxy.vaultQueryBy<IOUState>().states.filter { it.state.data.issuer.equals(proxy.nodeInfo().legalIdentities.first()) }
        return ResponseEntity.ok(myious)
    }
}
