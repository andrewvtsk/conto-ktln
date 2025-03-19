package com.ximedes.conto.web

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.TransferBuilder
import com.ximedes.conto.TransferFormBuilder
import com.ximedes.conto.UserBuilder
import com.ximedes.conto.core.service.AccountService
import com.ximedes.conto.core.service.TransferService
import com.ximedes.conto.core.service.UserService
import com.ximedes.conto.web.controller.TRANSFER_VIEW
import com.ximedes.conto.web.controller.TransferController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.ui.Model
import org.springframework.validation.BindingResult

class TransferControllerTest {

    val userService = mock<UserService>()
    val accountService = mock<AccountService>()
    val transferService = mock<TransferService>()
    val bindingResult = mock<BindingResult>()
    val model = mock<Model>()
    val accountIDCaptor = argumentCaptor<String>()
    val controller = TransferController(userService, accountService, transferService)

    @Test
    fun `it properly populates the account lists`() {
        val user = UserBuilder.build()
        val ownAccounts = AccountBuilder.build(3) {
            owner = user.username
        }
        val allAccounts = ownAccounts + AccountBuilder.build(7) {
            owner = " anotheruser"
        }

        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(ownAccounts)
        whenever(accountService.findAllAccounts()).thenReturn(allAccounts)
        whenever(transferService.findBalance(accountIDCaptor.capture())).thenReturn(123L)

        controller.populate(model)

        val capturedIDs = accountIDCaptor.allValues
        // It should only have retrieved balances for own accounts
        assertEquals(ownAccounts.size, capturedIDs.size)
        assertEquals(ownAccounts.map { it.accountID }.toSet(), capturedIDs.toSet())

        verify(model).addAttribute("userAccounts", ownAccounts)
        verify(model).addAttribute("allAccounts", allAccounts)

    }

    @Test
    fun `it stays on the page when the form is invalid`() {
        val form = TransferFormBuilder.build()
        whenever(bindingResult.hasErrors()).thenReturn(true)

        val mav = controller.transfer(form, bindingResult, model)

        assertEquals(TRANSFER_VIEW, mav.viewName)
        verifyNoInteractions(transferService)
    }

    @Test
    fun `it correctly maps form fields to service call parameters`() {
        val debitAccountCaptor = argumentCaptor<String>()
        val creditAccountCaptor = argumentCaptor<String>()
        val amountCaptor = argumentCaptor<Long>()
        val descriptionCaptor = argumentCaptor<String>()

        val form = TransferFormBuilder.build()
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(transferService.attemptTransfer(debitAccountCaptor.capture(), creditAccountCaptor.capture(), amountCaptor.capture(), descriptionCaptor.capture())).thenReturn(TransferBuilder.build(form))

        val mav = controller.transfer(form, bindingResult, model)

        assertEquals("redirect:/", mav.viewName)
        assertEquals(form.fromAccountID, debitAccountCaptor.firstValue)
        assertEquals(form.toAccountID, creditAccountCaptor.firstValue)
        assertEquals(form.amount, amountCaptor.firstValue)
        assertEquals(form.description, descriptionCaptor.firstValue)

    }

}