package com.ximedes.conto.adapter.output

import com.ximedes.conto.domain.Transfer
import com.ximedes.conto.db.TransferMapper
import com.ximedes.conto.core.port.output.TransferRepository
import org.springframework.stereotype.Repository

@Repository
class TransferRepositoryAdapter(private val transferMapper: TransferMapper) : TransferRepository {
    
    override fun saveTransfer(transfer: Transfer) {
        transferMapper.insertTransfer(transfer)
    }

    override fun findTransfersByAccountID(accountID: String): List<Transfer> {
        return transferMapper.findTransfersByAccountID(accountID)
    }
}
