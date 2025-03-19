package com.ximedes.conto.core.port.input

import com.ximedes.conto.core.domain.Transfer

interface TransferUseCase {
    fun attemptTransfer(
        debitAccountID: String,
        creditAccountID: String, 
        amount: Long, 
        description: String
    ): Transfer
    fun findTransfersByAccountID(accountID: String): List<Transfer>
    fun grantSignupBonus(accountID: String)
}