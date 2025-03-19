package com.ximedes.conto.web

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.UserBuilder
import com.ximedes.conto.core.domain.Account
import com.ximedes.conto.core.service.AccountService
import com.ximedes.conto.core.service.TransferService
import com.ximedes.conto.core.service.UserService
import com.ximedes.conto.web.controller.HomeController
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HomeControllerTest {

    val userService = mock<UserService>()
    val accountService = mock<AccountService>()
    val transferService = mock<TransferService>()
    val controller = HomeController(userService, accountService, transferService)

    @Test
    fun `it selects the user's first account when none is selected explicitly`() {
        val user = UserBuilder.build()
        val accounts = AccountBuilder.build(3) {
            owner = user.username
        }
        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(accounts)

        val mav = controller.get(null)
        val returnedAccounts = mav.model["accountList"] as List<Account>
        assertEquals(3, returnedAccounts.size)
        assertEquals(accounts[0].accountID, mav.model["selectedAccountID"])
    }

    @Test
    fun `it selects the user's first account when the selected account ID is not owned by user`() {
        val user = UserBuilder.build()
        val accounts = AccountBuilder.build(3) {
            owner = user.username
        }
        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(accounts)

        val mav = controller.get("someotheraccountid")
        val returnedAccounts = mav.model["accountList"] as List<Account>
        assertEquals(3, returnedAccounts.size)
        assertEquals(accounts[0].accountID, mav.model["selectedAccountID"])
    }

    @Test
    fun `it selects the selected account ID if owned by user`() {
        val user = UserBuilder.build()
        val accounts = AccountBuilder.build(3) {
            owner = user.username
        }
        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(accounts)

        val mav = controller.get(accounts[1].accountID)

        val returnedAccounts = mav.model["accountList"] as List<Account>
        assertEquals(3, returnedAccounts.size)
        assertEquals(accounts[1].accountID, mav.model["selectedAccountID"])
    }

}