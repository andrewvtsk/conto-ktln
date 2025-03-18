package com.ximedes.conto.core.port.output

import com.ximedes.conto.domain.Transfer

interface TransferRepository {
    fun saveTransfer(transfer: Transfer)
    fun findTransfersByAccountID(accountID: String): List<Transfer>
}