package com.ximedes.conto.domain

import java.time.LocalDateTime

data class Transfer(val transferID: Long,
                    val debitAccountID: String,
                    val creditAccountID: String,
                    val amount: Long,
                    val description: String,
                    val timestamp: LocalDateTime = LocalDateTime.now()) {

    constructor(debitAccountID: String, creditAccountID: String, amount: Long, description: String) : this(-1L, debitAccountID, creditAccountID, amount, description, LocalDateTime.now())
}