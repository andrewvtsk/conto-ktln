package com.ximedes.conto.web

import com.nhaarman.mockitokotlin2.*
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.UserBuilder
import com.ximedes.conto.core.domain.Account
import com.ximedes.conto.core.domain.Transfer
import com.ximedes.conto.core.port.output.AccountBalancePort
import com.ximedes.conto.core.service.AccountService
import com.ximedes.conto.core.service.TransferService
import com.ximedes.conto.core.service.UserService
import com.ximedes.conto.web.controller.HomeController
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class HomeControllerTest {

    private val userService = mock<UserService>()
    private val accountService = mock<AccountService>()
    private val accountBalancePort = mock<AccountBalancePort>()
    private val transferService = mock<TransferService>()

    private val controller = HomeController(userService, accountService, accountBalancePort, transferService)

    @BeforeEach
    fun setup() {
        Mockito.reset(userService, accountService, accountBalancePort, transferService)
    }

    @Test
    fun `it selects the user's first account when none is selected explicitly`() {
        val user = UserBuilder.build()
        val accounts = AccountBuilder.build(3) { owner = user.username }
        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(accounts)
        whenever(accountBalancePort.getBalance(any())).thenReturn(100L)
        whenever(transferService.findTransfersByAccountID(any())).thenReturn(emptyList())

        val mav = controller.get(null)

        val returnedAccounts = mav.model["accountList"] as List<Account>
        assertEquals(3, returnedAccounts.size)
        assertEquals(accounts[0].accountID, mav.model["selectedAccountID"])

        val balances = mav.model["balances"] as Map<String, Long>
        assertEquals(3, balances.size)
        assertTrue(balances.values.all { it == 100L })
    }

    @Test
    fun `it selects the user's first account when the selected account ID is not owned by user`() {
        val user = UserBuilder.build()
        val accounts = AccountBuilder.build(3) { owner = user.username }
        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(accounts)
        whenever(accountBalancePort.getBalance(any())).thenReturn(100L)
        whenever(transferService.findTransfersByAccountID(any())).thenReturn(emptyList())

        val mav = controller.get("someotheraccountid")

        val returnedAccounts = mav.model["accountList"] as List<Account>
        assertEquals(3, returnedAccounts.size)
        assertEquals(accounts[0].accountID, mav.model["selectedAccountID"])
    }

    @Test
    fun `it selects the selected account ID if owned by user`() {
        val user = UserBuilder.build()
        val accounts = AccountBuilder.build(3) { owner = user.username }
        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(accounts)
        whenever(accountBalancePort.getBalance(any())).thenReturn(100L)
        whenever(transferService.findTransfersByAccountID(any())).thenReturn(emptyList())

        val mav = controller.get(accounts[1].accountID)

        val returnedAccounts = mav.model["accountList"] as List<Account>
        assertEquals(3, returnedAccounts.size)
        assertEquals(accounts[1].accountID, mav.model["selectedAccountID"])
    }

    @Test
    fun `it correctly retrieves balances for user accounts`() {
        val user = UserBuilder.build()
        val accounts = AccountBuilder.build(3) { owner = user.username }
        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(accounts)
        whenever(accountBalancePort.getBalance(any())).thenReturn(500L)

        val mav = controller.get(null)

        val balances = mav.model["balances"] as Map<String, Long>
        assertEquals(3, balances.size)
        assertTrue(balances.values.all { it == 500L })
    }

    @Test
    fun `it retrieves transfers for the selected account`() {
        val user = UserBuilder.build()
        val accounts = AccountBuilder.build(3) { owner = user.username }
        val selectedAccountID = accounts[0].accountID
        val expectedTransfers = listOf(
            Transfer(selectedAccountID, "NLBRAT00000002", 100L, "Transfer 1"),
            Transfer("NLBRAT00000003", selectedAccountID, 200L, "Transfer 2")
        )

        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(accounts)
        whenever(accountBalancePort.getBalance(any())).thenReturn(500L)
        whenever(transferService.findTransfersByAccountID(selectedAccountID)).thenReturn(expectedTransfers)

        val mav = controller.get(selectedAccountID)

        val transfers = mav.model["transfers"] as? List<Transfer> ?: emptyList()
        assertEquals(2, transfers.size)
        assertEquals(expectedTransfers, transfers)
    }

    @Test
    fun `it retrieves all accounts for address book`() {
        val user = UserBuilder.build()
        val accounts = AccountBuilder.build(3) { owner = user.username }

        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(accounts)
        whenever(accountBalancePort.getBalance(any())).thenReturn(100L)

        val mav = controller.get(null)

        val allAccounts = mav.model["allAccounts"] as Map<String, Account>
        assertEquals(3, allAccounts.size)
        assertTrue(allAccounts.keys.containsAll(accounts.map { it.accountID }))
    }
}
