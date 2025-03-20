package com.ximedes.conto.db

import com.ximedes.conto.AbstractIntegrationTest
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.core.domain.Account
import com.ximedes.conto.core.domain.AccountCriteria
import com.ximedes.conto.core.domain.Transfer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired


class AccountMapperTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var accountMapper: AccountMapper

    @Autowired
    lateinit var transferMapper: TransferMapper

    val accountOwner = "accounttest"

    @BeforeAll
    fun insertUser() {
        createUser(accountOwner)
    }

    @BeforeEach
    fun setup() {
        val debitAccount = Account("NLBRAT000001", "test_user", "Checking", 0L, 1000L)
        val creditAccount = Account("NLBRAT000002", "test_user", "Savings", 0L, 500L)

        if (accountMapper.find(AccountCriteria(null, "NLBRAT000001")).isEmpty()) {
            accountMapper.insertAccount(debitAccount)
            accountMapper.setAccountBalance(debitAccount.accountID, 1000L)
        }

        if (accountMapper.find(AccountCriteria(null, "NLBRAT000002")).isEmpty()) {
            accountMapper.insertAccount(creditAccount)
            accountMapper.setAccountBalance(creditAccount.accountID, 500L)
        }

        if (transferMapper.findByTransferID(1) == null) {
            val transfer = Transfer("NLBRAT000001", "NLBRAT000002", 100L, "Test transfer")
            transferMapper.insertTransfer(transfer)
        }
    }

    @Test
    fun `basic insert, update and find functions work`() {
        val account = AccountBuilder.build {
            owner = accountOwner
            balance = 500L
        }.also {
            accountMapper.insertAccount(it)
        }

        // Find by account ID
        accountMapper.find(AccountCriteria(null, account.accountID)).let {
            assertEquals(account, it.single())
        }

        val updated = account.copy(description = "A.C. Count", minimumBalance = -100L)
        accountMapper.updateAccount(updated)

        // Find by owner
        accountMapper.find(AccountCriteria(accountOwner, null)).let {
            assertEquals(updated, it.single())
        }
    }

    @Test
    fun `can retrieve account owner by account ID`() {
        val testAccount = Account("NLBRAT000001", "test_user", "Test Account", 0L, 0L)
        accountMapper.insertAccount(testAccount)

        val owner = accountMapper.findAccountOwnerById(testAccount.accountID)
        assertEquals("test_user", owner)
    }

    @Test
    fun `can retrieve account balance by account ID`() {
        val initialBalance = 1000L
        val account = AccountBuilder.build {
            owner = accountOwner
            balance = initialBalance
        }.also {
            accountMapper.insertAccount(it)
        }

        val balance = accountMapper.findBalanceByAccountId(account.accountID)
        assertEquals(initialBalance, balance)
    }

    @Test
    fun `update account balance when transferring money`() {
        val initialBalanceDebit = 1000L
        val initialBalanceCredit = 500L
        val amountToTransfer = 200L
    
        val debitAccount = AccountBuilder.build {
            owner = accountOwner
            balance = initialBalanceDebit
        }.also {
            accountMapper.insertAccount(it)
        }
    
        val creditAccount = AccountBuilder.build {
            owner = accountOwner
            balance = initialBalanceCredit
        }.also {
            accountMapper.insertAccount(it)
        }
    
        accountMapper.updateAccountBalanceWhenTransfer(debitAccount.accountID, -amountToTransfer)
        accountMapper.updateAccountBalanceWhenTransfer(creditAccount.accountID, amountToTransfer)
    
        val updatedBalanceDebit = accountMapper.findBalanceByAccountId(debitAccount.accountID)
        val updatedBalanceCredit = accountMapper.findBalanceByAccountId(creditAccount.accountID)
    
        assertEquals(initialBalanceDebit - amountToTransfer, updatedBalanceDebit)
        assertEquals(initialBalanceCredit + amountToTransfer, updatedBalanceCredit)
    }    

    @Test
    fun `optimistic locking prevents stale updates on credit`() {
        val initialBalance = 500L
        val creditAmount = 200L
        val initialVersion = 1L // Указываем начальную версию

        val account = AccountBuilder.build {
            owner = accountOwner
            balance = initialBalance
        }.also {
            accountMapper.insertAccount(it)
            accountMapper.setAccountBalance(it.accountID, initialBalance) // Устанавливаем начальный баланс
        }

        // Обновляем баланс с проверкой версии
        val updatedRows = accountMapper.updateBalanceCreditAccountWithOptimisticLock(account.accountID, creditAmount)
        assertEquals(1, updatedRows)

        val newBalance = accountMapper.findBalanceByAccountId(account.accountID)
        assertEquals(initialBalance + creditAmount, newBalance)
    }

    @Test
    fun `optimistic locking prevents stale updates on debit`() {
        val initialBalance = 500L
        val debitAmount = 200L
        val account = AccountBuilder.build {
            owner = accountOwner
            balance = initialBalance
        }.also {
            accountMapper.insertAccount(it)
        }

        val updatedRows = accountMapper.updateBalanceDebitAccountWithOptimisticLock(account.accountID, debitAmount)
        assertEquals(1, updatedRows)

        val newBalance = accountMapper.findBalanceByAccountId(account.accountID)
        assertEquals(initialBalance - debitAmount, newBalance)
    }

    @Test
    fun `optimistic locking prevents overdraft beyond minimum balance`() {
        var minimumBalance = -500L
        val initialBalance = 100L
        val overdraftAmount = 700L
        val account = AccountBuilder.build {
            owner = accountOwner
            balance = initialBalance
            minimumBalance = minimumBalance
        }.also {
            accountMapper.insertAccount(it)
        }

        val updatedRows = accountMapper.updateBalanceDebitAccountWithOptimisticLock(account.accountID, overdraftAmount)
        assertEquals(0, updatedRows)

        val newBalance = accountMapper.findBalanceByAccountId(account.accountID)
        assertEquals(initialBalance, newBalance)
    }
}
