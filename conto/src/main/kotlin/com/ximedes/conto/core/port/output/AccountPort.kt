package com.ximedes.conto.core.port.output

import com.ximedes.conto.core.domain.Account

interface AccountPort {
    fun findByOwner(owner: String): List<Account>
    fun findById(accountID: String): Account?
    fun save(account: Account)
    fun getNextUniqueID(): Long
}
