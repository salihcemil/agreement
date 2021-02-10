package com.isbank.agreement.flows

import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub

object NotaryUtils {
    /**
     * Retrieves the Notary primarily from the CorDapp configuration file.
     * If the file cannot be found, we fetch the first notary identity in the known notary identities list.
     */
    fun getNotary(serviceHub: ServiceHub): Party? {
        val cordappConfig = serviceHub.cordappProvider.getAppContext().config
        val notaryName: CordaX500Name?
        notaryName = if (cordappConfig.exists("notaryName")) {
            CordaX500Name.parse(cordappConfig.getString("notaryName"))
        } else {
            if (serviceHub.networkMapCache.notaryIdentities.size > 0) serviceHub.networkMapCache.notaryIdentities[0].name else null
        }
        return if (notaryName != null) {
            serviceHub.networkMapCache.getNotary(notaryName)
        } else {
            throw IllegalStateException("Unable to find a notary identity on the network.")
        }
    }
}