package com.ximedes.conto.adapter.output

import com.ximedes.conto.core.domain.Account
import com.ximedes.conto.core.domain.AccountCriteria
import com.ximedes.conto.core.port.output.AccountPort
import com.ximedes.conto.db.AccountMapper
import org.springframework.stereotype.Repository

@Repository
class AccountAdapter(private val accountMapper: AccountMapper) : AccountPort {

    override fun findByOwner(owner: String): List<Account> {
        return accountMapper.find(AccountCriteria(ownerID = owner))
    }

    override fun findById(accountID: String): Account? {
        return accountMapper.find(AccountCriteria(null, accountID)).firstOrNull()
    }

    override fun save(account: Account) {
        accountMapper.insertAccount(account)
    }

    override fun getNextUniqueID(): Long {
        return accountMapper.getNextUniqueID()
    }
}
