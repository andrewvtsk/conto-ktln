package com.ximedes.conto.db

import com.ximedes.conto.AbstractIntegrationTest
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.core.domain.AccountCriteria
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AccountMapperTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var accountMapper: AccountMapper

    val accountOwner = "accounttest"

    @BeforeAll
    fun insertUser() {
        createUser(accountOwner)
    }

    @Test
    fun `basic insert, update and find functions work`() {
        val account = AccountBuilder.build {
            owner = accountOwner
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
}