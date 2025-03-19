package com.ximedes.conto.web.controller

import com.ximedes.conto.core.domain.isAdmin
import com.ximedes.conto.core.service.AccountService
import com.ximedes.conto.core.service.UserService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import javax.validation.Valid

const val ACCOUNT_VIEW = "account"

@Controller
@RequestMapping("/account")
class AccountController(private val accountService: AccountService, private val userService: UserService) {

    @ModelAttribute("createAccountForm")
    fun createAccountForm() = if (userService.loggedInUser.isAdmin) {
        AdminCreateAccountForm()
    } else {
        CreateAccountForm()
    }

    @GetMapping
    fun get() = ACCOUNT_VIEW

    @PostMapping
    fun createAccount(@Valid form: CreateAccountForm, bindingResult: BindingResult, model: Model): ModelAndView {
        return if (bindingResult.hasErrors()) {
            ModelAndView(ACCOUNT_VIEW, model.asMap())
        } else {
            if (userService.loggedInUser.isAdmin) {
                val aFrm = form as AdminCreateAccountForm
                try {
                    // Note that we can safely force non-null here because of the successful validation
                    accountService.createAccount(aFrm.ownerID!!, aFrm.description!!, aFrm.minimumBalance!!)
                } catch (e: UsernameNotFoundException) {
                    bindingResult.addError(
                        FieldError(
                            "createAccountForm",
                            "ownerID",
                            aFrm.ownerID,
                            false,
                            null,
                            null,
                            "User does not exit"
                        )
                    )
                    return ModelAndView(ACCOUNT_VIEW, model.asMap())
                }
            } else {
                accountService.createAccount(form.description!!)
            }
            ModelAndView("redirect:/")
        }
    }

}