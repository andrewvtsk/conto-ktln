package com.ximedes.conto.web

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.ximedes.conto.AccountFormBuilder
import com.ximedes.conto.UserBuilder
import com.ximedes.conto.core.domain.Role
import com.ximedes.conto.core.service.AccountService
import com.ximedes.conto.core.service.UserService
import com.ximedes.conto.web.controller.ACCOUNT_VIEW
import com.ximedes.conto.web.controller.AccountController
import com.ximedes.conto.web.controller.AdminCreateAccountForm
import com.ximedes.conto.web.controller.CreateAccountForm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.ui.Model
import org.springframework.validation.BindingResult

class AccountControllerTest {

    val userService = mock<UserService>()
    val accountService = mock<AccountService>()
    val bindingResult = mock<BindingResult>()
    val model = mock<Model>()
    val controller = AccountController(accountService, userService)

    @Test
    fun `it returns the correct account form based on role`() {
        val admin = UserBuilder.build {
            role = Role.ADMIN
        }
        val regular = UserBuilder.build {
            role = Role.USER
        }

        whenever(userService.loggedInUser).thenReturn(admin).thenReturn(regular)
        assertTrue(controller.createAccountForm() is AdminCreateAccountForm)
        assertTrue(controller.createAccountForm() !is AdminCreateAccountForm)
    }

    @Test
    fun `on error it shows the account view again with the same model`() {
        val af = CreateAccountForm()
        val modelAsMap = emptyMap<String, Any>()
        whenever(bindingResult.hasErrors()).thenReturn(true)
        whenever(model.asMap()).thenReturn(modelAsMap)

        val mav = controller.createAccount(af, bindingResult, model)
        assertEquals(ACCOUNT_VIEW, mav.viewName)
        assertEquals(modelAsMap, mav.model)
    }

    @Test
    fun `create account as admin user`() {
        val form = AccountFormBuilder.buildAdmin()
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(userService.loggedInUser).thenReturn(UserBuilder.build { role = Role.ADMIN })

        val mav = controller.createAccount(form, bindingResult, model)

        verify(accountService).createAccount(form.ownerID!!, form.description!!, form.minimumBalance!!)
        assertEquals("redirect:/", mav.viewName)
    }

    @Test
    fun `it stays on the current page when creating an account as admin user for non-existing user`() {
        val form = AccountFormBuilder.buildAdmin()
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(userService.loggedInUser).thenReturn(UserBuilder.build { role = Role.ADMIN })
        whenever(accountService.createAccount(form.ownerID!!, form.description!!, form.minimumBalance!!)).thenThrow(UsernameNotFoundException("expected"))

        val mav = controller.createAccount(form, bindingResult, model)

        assertEquals(ACCOUNT_VIEW, mav.viewName)
        verify(bindingResult).addError(any())
    }

    @Test
    fun `create account as normal user`() {
        val form = AccountFormBuilder.build()
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(userService.loggedInUser).thenReturn(UserBuilder.build())

        val mav = controller.createAccount(form, bindingResult, model)

        verify(accountService).createAccount(form.description!!)
        assertEquals("redirect:/", mav.viewName)
    }

    @Test
    fun `it shows the right page in a GET request`() {
        assertEquals(ACCOUNT_VIEW, controller.get())
    }

}