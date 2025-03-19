package com.ximedes.conto.web.controller

import com.ximedes.conto.core.service.UserService
import mu.KotlinLogging
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import javax.validation.Valid

private const val SIGNUP_VIEW = "signup"

@Controller
@RequestMapping("/public/signup")
class SignupController(private val userService: UserService) {

    private val logger = KotlinLogging.logger { }

    @ModelAttribute("signupForm")
    fun createForm() = SignupForm()

    @GetMapping
    fun get() = SIGNUP_VIEW

    @PostMapping
    fun createAccount(@Valid form: SignupForm, bindingResult: BindingResult, model: Model) =
        if (bindingResult.hasErrors()) {
            ModelAndView(SIGNUP_VIEW, model.asMap())
        } else {
            userService.signupAndLogin(form.username!!, form.password!!)
            ModelAndView("redirect:/")
        }

}