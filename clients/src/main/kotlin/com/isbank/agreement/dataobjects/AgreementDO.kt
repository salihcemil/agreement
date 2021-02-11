package com.isbank.agreement.dataobjects

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import java.util.*

class AgreementDO(val issuerName: String,
                  val acquirerName: String,
                  val pan : String,
                  val timeAndDate: Date,
                  val validUntil : Date,
                  val amount: Amount<Currency>,
                  val linearId: UniqueIdentifier) {

}