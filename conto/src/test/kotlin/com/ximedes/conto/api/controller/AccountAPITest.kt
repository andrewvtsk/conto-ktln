package com.ximedes.conto.api.controller

import com.nhaarman.mockitokotlin2.*
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.UserBuilder
import com.ximedes.conto.core.port.input.AccountUseCase
import com.ximedes.conto.core.port.output.AccountBalancePort
import com.ximedes.conto.core.service.UserService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AccountAPITest {

    private val accountService = mock<AccountUseCase>()
    private val accountBalancePort = mock<AccountBalancePort>()
    private val userService = mock<UserService>()
    private val api = AccountAPI(accountService, accountBalancePort, userService)

    @Test
    fun `empty account list returns empty response list`() {
        whenever(userService.loggedInUser).thenReturn(UserBuilder.build())
        whenever(accountService.findByOwner(any())).thenReturn(emptyList())

        val response = api.findAccounts()
        assertEquals(HttpStatus.OK, response.statusCode)
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
        whenever(accountService.findByOwner(user.username)).thenReturn(listOf(account))
        whenever(accountBalancePort.getBalance(account.accountID)).thenReturn(543L)

        val response = api.findAccounts()

        val fromResponse = response.body!![0]
        assertAll(
            { assertEquals(account.accountID, fromResponse.accountID) },
            { assertEquals(account.description, fromResponse.description) },
            { assertEquals(account.owner, fromResponse.owner) },
            { assertEquals(account.minimumBalance, fromResponse.minimumBalanceAllowed) },
            { assertEquals(543L, fromResponse.balance) }
        )
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
        whenever(accountService.findByOwner(user.username)).thenReturn(listOf(b))
        whenever(accountBalancePort.getBalance(b.accountID)).thenReturn(1234L)

        val response = api.findAccounts()
        val accounts = response.body!!
        assertEquals(1, accounts.size)

        assertEquals(1234L, accounts[0].balance)
        assertEquals(-999L, accounts[0].minimumBalanceAllowed)
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
        whenever(accountService.findByOwner(user.username)).thenReturn(listOf(account))
        whenever(accountBalancePort.getBalance(account.accountID)).thenReturn(22L)

        val response = api.findAccounts()
        val accounts = response.body!!

        assertEquals(22L, accounts[0].balance)
    }

    @Test
    fun `returns bad request when user is not logged in`() {
        whenever(userService.loggedInUser).thenReturn(null)

        val response = api.findAccounts()

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNull(response.body)
    }
}
