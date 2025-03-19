package com.ximedes.conto.db

import com.ximedes.conto.core.domain.Account
import com.ximedes.conto.core.domain.AccountCriteria
import com.ximedes.conto.core.domain.Transfer
import com.ximedes.conto.core.domain.User
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.springframework.stereotype.Repository

/**
 * IntelliJ IDEA does not understand that the @Mapper
 * annotation means these mappers can be injected. The
 * only reason we're adding @Repository here is to repair
 * that issue
 */
@Repository
@Mapper
private annotation class MyBatisMapper

@MyBatisMapper
interface UserMapper {
    fun findByUsername(username: String): User?
    fun insertUser(@Param("user") user: User, @Param("canonicalUsername") canonicalUsername: String)
    fun isCommonPassword(password: String): Boolean
}

@MyBatisMapper
interface AccountMapper {
    fun insertAccount(account: Account)
    fun updateAccount(account: Account)
    fun setAccountBalance(@Param("accountId") accountId: String, @Param("balance") balance: Long)
    fun updateAccountBalanceWhenTransfer(@Param("accountId") accountId: String, @Param("amount") amount: Long)
    fun find(criteria: AccountCriteria): List<Account>
    fun getNextUniqueID(): Long
    fun findAccountOwnerById(accountId: String): String
    fun findBalanceByAccountId(accountId: String): Long
    fun updateBalanceWithOptimisticLock(@Param("accountId") accountId: String, @Param("amount") amount: Long): Int
}

@MyBatisMapper
interface TransferMapper {
    fun insertTransfer(t: Transfer)
    fun findByTransferID(transferID: Long): Transfer?
    fun findTransfersByAccountID(accountID: String): List<Transfer>
    fun calculateBalanceByAccountID(accountID: String): Long
}