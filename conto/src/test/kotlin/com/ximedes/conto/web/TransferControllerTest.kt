package com.ximedes.conto.web

import com.nhaarman.mockitokotlin2.*
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.TransferBuilder
import com.ximedes.conto.TransferFormBuilder
import com.ximedes.conto.UserBuilder
import com.ximedes.conto.core.domain.AccountNotAvailableException
import com.ximedes.conto.core.domain.AccountNotAvailableException.Type.CREDIT
import com.ximedes.conto.core.domain.AccountNotAvailableException.Type.DEBIT
import com.ximedes.conto.core.domain.InsufficientFundsException
import com.ximedes.conto.core.port.output.AccountBalancePort
import com.ximedes.conto.core.service.AccountService
import com.ximedes.conto.core.service.TransferService
import com.ximedes.conto.core.service.UserService
import com.ximedes.conto.web.controller.TRANSFER_VIEW
import com.ximedes.conto.web.controller.TransferController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError


class TransferControllerTest {

    private val userService = mock<UserService>()
    private val accountService = mock<AccountService>()
    private val accountBalancePort = mock<AccountBalancePort>()
    private val transferService = mock<TransferService>()
    private val bindingResult = mock<BindingResult>()
    private val model = mock<Model>()

    private val accountIDCaptor = argumentCaptor<String>()
    private val controller = TransferController(userService, accountService, accountBalancePort, transferService)

    @Test
    fun `it properly populates the account lists`() {
        val user = UserBuilder.build()
        val ownAccounts = AccountBuilder.build(3) { owner = user.username }

        whenever(userService.loggedInUser).thenReturn(user)
        whenever(accountService.findByOwner(user.username)).thenReturn(ownAccounts)
        whenever(accountBalancePort.getBalance(any())).thenAnswer { 123L }

        controller.populate(model)

        val expectedBalances = ownAccounts.associate { it.accountID to 123L }

        verify(model).addAttribute("userAccounts", ownAccounts)
        verify(model).addAttribute("balances", expectedBalances)
        verify(model).addAttribute("allAccounts", ownAccounts)
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
        whenever(
            transferService.attemptTransfer(
                debitAccountCaptor.capture(),
                creditAccountCaptor.capture(),
                amountCaptor.capture(),
                descriptionCaptor.capture()
            )
        ).thenReturn(TransferBuilder.build(form))

        val mav = controller.transfer(form, bindingResult, model)

        assertEquals("redirect:/", mav.viewName)
        assertEquals(form.fromAccountID, debitAccountCaptor.firstValue)
        assertEquals(form.toAccountID, creditAccountCaptor.firstValue)
        assertEquals(form.amount, amountCaptor.firstValue)
        assertEquals(form.description, descriptionCaptor.firstValue)
    }

    @Test
    fun `it handles insufficient funds exception`() {
        val form = TransferFormBuilder.build()
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(
            transferService.attemptTransfer(
                any(), any(), any(), any()
            )
        ).thenThrow(InsufficientFundsException("Insufficient funds"))

        val mav = controller.transfer(form, bindingResult, model)

        assertEquals(TRANSFER_VIEW, mav.viewName)
        verify(bindingResult).addError(
            ArgumentMatchers.argThat { it as FieldError
                it.objectName == "transferForm" && it.field == "amount" && it.defaultMessage == "Insufficient funds"
            }
        )
    }

    @Test
    fun `it handles account not available exception for debit`() {
        val form = TransferFormBuilder.build()
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(
            transferService.attemptTransfer(
                any(), any(), any(), any()
            )
        ).thenThrow(AccountNotAvailableException(DEBIT, "Debit account not found"))

        val mav = controller.transfer(form, bindingResult, model)

        assertEquals(TRANSFER_VIEW, mav.viewName)
        verify(bindingResult).addError(
            ArgumentMatchers.argThat { it as FieldError
                it.objectName == "transferForm" && it.field == "fromAccountID" && it.defaultMessage == "Debit account not found"
            }
        )
    }

    @Test
    fun `it handles account not available exception for credit`() {
        val form = TransferFormBuilder.build()
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(
            transferService.attemptTransfer(
                any(), any(), any(), any()
            )
        ).thenThrow(AccountNotAvailableException(CREDIT, "Credit account not found"))

        val mav = controller.transfer(form, bindingResult, model)

        assertEquals(TRANSFER_VIEW, mav.viewName)
        verify(bindingResult).addError(
            ArgumentMatchers.argThat { it as FieldError
                it.objectName == "transferForm" && it.field == "toAccountID" && it.defaultMessage == "Credit account not found"
            }
        )
    }
}
