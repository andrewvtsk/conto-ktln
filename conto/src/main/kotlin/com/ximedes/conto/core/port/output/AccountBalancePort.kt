package com.ximedes.conto.core.port.output

interface AccountBalancePort {
    fun getBalance(accountId: String): Long
    fun updateBalance(accountId: String, amount: Long): Boolean
    fun getAccountOwner(accountId: String): String? 
}