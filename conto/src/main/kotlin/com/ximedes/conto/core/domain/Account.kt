package com.ximedes.conto.core.domain

data class Account(
    val accountID: String, 
    val owner: String, 
    val description: String, 
    val minimumBalance: Long, 
    val balance: Long? = 0L
)

data class AccountCriteria(val ownerID: String? = null, val accountID: String? = null)