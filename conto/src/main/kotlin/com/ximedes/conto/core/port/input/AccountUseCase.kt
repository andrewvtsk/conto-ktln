package com.ximedes.conto.core.port.input

import com.ximedes.conto.core.domain.Account

interface AccountUseCase {
    fun getRootAccount(): Account
    fun createAccount(description: String): Account
    fun createAccount(ownerName: String, description: String, minimumBalance: Long): Account
    fun findByOwner(user: String): List<Account>
}
