package com.ximedes.conto.web.controller

import com.ximedes.conto.domain.AccountNotAvailableException
import com.ximedes.conto.domain.AccountNotAvailableException.Type.CREDIT
import com.ximedes.conto.domain.AccountNotAvailableException.Type.DEBIT
import com.ximedes.conto.domain.InsufficientFundsException
import com.ximedes.conto.service.AccountService
import com.ximedes.conto.service.TransferService
import com.ximedes.conto.service.UserService
import mu.KotlinLogging
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

const val TRANSFER_VIEW = "transfer"

@Controller
@RequestMapping("/transfer")
class TransferController(
    private val userService: UserService,
    private val accountService: AccountService,
    private val transferService: TransferService
) {

    private val logger = KotlinLogging.logger { }

    @ModelAttribute
    fun populate(model: Model) {
        val currentUser = userService.loggedInUser!!
        logger.debug("Retrieving accounts owned by user ${currentUser.username}")
        val accountsForUser = accountService.findByOwner(currentUser.username)
        model.addAttribute("userAccounts", accountsForUser)
        logger.debug("Retrieving balances for ${accountsForUser.size} accounts owned by user ${currentUser.username}")
        val balances = accountsForUser.groupBy(keySelector = { it.accountID }, valueTransform = {
            accountService.getBalance(it.accountID)
        })        
        model.addAttribute("balances", balances)
        logger.debug("Adding all accounts to the model")
        val allAccounts = accountService.findAllAccounts()
        model.addAttribute("allAccounts", allAccounts)
    }

    @GetMapping
    fun get(): ModelAndView {
        val mav = ModelAndView(TRANSFER_VIEW)
        mav.addObject(TransferForm())
        return mav
    }

    @PostMapping
    fun transfer(@Valid form: TransferForm, bindingResult: BindingResult, model: Model): ModelAndView {
        if (bindingResult.hasErrors()) {
            return ModelAndView("transfer", model.asMap())
        }
        try {
            transferService.attemptTransfer(form.fromAccountID!!, form.toAccountID!!, form.amount!!, form.description!!)
            return ModelAndView("redirect:/")
        } catch (e: InsufficientFundsException) {
            bindingResult.addFieldError("transferForm", "amount", form.amount!!, "Insufficient funds")
        } catch (e: AccountNotAvailableException) {
            when (e.type) {
                DEBIT -> bindingResult.addFieldError("transferForm", "fromAccountID", form.fromAccountID!!, e.message)
                CREDIT -> bindingResult.addFieldError("transferForm", "toAccountID", form.toAccountID!!, e.message)
            }
        }
        return ModelAndView("transfer", model.asMap())
    }
}

private fun BindingResult.addFieldError(objectName: String, field: String, rejectedValue: Any, message: String?) {
    this.addError(FieldError(objectName, field, rejectedValue, false, null, null, message))
}
