package com.ximedes.conto.adapter.output

import com.ximedes.conto.core.port.output.AccountBalancePort
import com.ximedes.conto.db.AccountMapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Update
import org.springframework.stereotype.Repository

@Repository
class AccountBalanceAdapter(private val accountMapper: AccountMapper) : AccountBalancePort {

    override fun getBalance(accountId: String): Long {
        return accountMapper.findBalanceByAccountId(accountId) ?: 0L
    }

    override fun updateBalance(accountId: String, amount: Long): Boolean {
        return accountMapper.updateBalanceWithOptimisticLock(accountId, amount) > 0
    }

    override fun getAccountOwner(accountId: String): String? {
        return accountMapper.findAccountOwnerById(accountId)
    }
}