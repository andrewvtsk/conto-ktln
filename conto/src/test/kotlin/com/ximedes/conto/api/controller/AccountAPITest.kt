package com.ximedes.conto.api.controller

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.UserBuilder
import com.ximedes.conto.core.service.AccountService
import com.ximedes.conto.core.service.TransferService
import com.ximedes.conto.core.service.UserService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AccountAPITest {

    val accountService = mock<AccountService>()
    val userService = mock<UserService>()
    val transferService = mock<TransferService>()
    val api = AccountAPI(accountService, transferService, userService)

    @Test
    fun `empty account list returns empty response list`() {
        whenever(userService.loggedInUser).thenReturn(UserBuilder.build())
        whenever(accountService.findAllAccounts()).thenReturn(emptyList())

        val response = api.findAccounts()
        assertNotNull(response.body)
        assertTrue(response.body!!.isEmpty())
    }

    @Test
    fun `it maps account fields properly to the DTO`() {
        val user = UserBuilder.build()
        val account = AccountBuilder.build {
            owner = user.username
        }

        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findAllAccounts()).thenReturn(listOf(account))
        whenever(transferService.calculateBalanceByAccountID(account.accountID)).thenReturn(543L)

        val response = api.findAccounts()

        val fromResponse = response.body!![0]
        assertAll(
            { assertEquals(account.accountID, fromResponse.accountID) },
            { assertEquals(account.description, fromResponse.description) },
            { assertEquals(account.owner, fromResponse.owner) },
            { assertEquals(account.minimumBalance, fromResponse.minimumBalanceAllowed) },
            { assertEquals(543L, fromResponse.balance) })

    }

    @Test
    fun `only accounts owned by the current user contain balance information`() {
        val user = UserBuilder.build()
        val a = AccountBuilder.build()
        val b = AccountBuilder.build {
            owner = user.username
            minimumBalance = -999L
        }
        val c = AccountBuilder.build()

        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findAllAccounts()).thenReturn(listOf(a, b, c))
        whenever(transferService.calculateBalanceByAccountID(b.accountID)).thenReturn(1234L)

        val response = api.findAccounts()
        val accounts = response.body!!
        assertEquals(3, accounts.size)

        assertNull(accounts[0].balance)
        assertNull(accounts[0].minimumBalanceAllowed)
        assertEquals(1234L, accounts[1].balance)
        assertEquals(-999L, accounts[1].minimumBalanceAllowed)
        assertNull(accounts[2].balance)
        assertNull(accounts[2].minimumBalanceAllowed)
    }

    @Test
    fun `user account contains balance`() {
        val user = UserBuilder.build()

        val account = AccountBuilder.build {
            owner = user.username
            minimumBalance = -999L
            balance = 22L
        }

        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findAllAccounts()).thenReturn(listOf(account))

        val response = api.findAccounts()
        val accounts = response.body!!

        assertEquals(22L, accounts[0].balance)
    }
}