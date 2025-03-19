package com.ximedes.conto.core.port.output

interface AccountBalancePort {
    fun getBalance(accountId: String): Long
    fun getAccountOwner(accountId: String): String? 
    fun updateBalanceDebit(accountId: String, amount: Long): Boolean
    fun updateBalanceCredit(accountId: String, amount: Long): Boolean
}