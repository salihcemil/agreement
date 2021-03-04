package com.isbank.agreement.dataobjects

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import java.util.*

class IOUDO (val issuerName: String,
             val acquirerName: String,
             val amount: Amount<Currency>,
             val agreementStateID: UUID,
             val linearId: UniqueIdentifier){
}