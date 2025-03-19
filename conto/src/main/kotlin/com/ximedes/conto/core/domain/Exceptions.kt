package com.ximedes.conto.core.domain

class AccountNotAvailableException(val type: Type, s: String) : RuntimeException(s) {
    enum class Type {
        UNKNOWN,DEBIT, CREDIT
    }
}

class InsufficientFundsException(s: String): RuntimeException(s)