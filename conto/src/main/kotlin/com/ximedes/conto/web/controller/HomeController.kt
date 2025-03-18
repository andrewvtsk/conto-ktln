package com.ximedes.conto.web.controller

import com.ximedes.conto.service.AccountService
import com.ximedes.conto.service.TransferService
import com.ximedes.conto.service.UserService
import mu.KotlinLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView

private const val ACCOUNT_LIST_KEY = "accountList"
private const val BALANCES_KEY = "balances";
private const val SELECTED_ACCOUNT_ID_KEY = "selectedAccountID";
private const val ALL_ACCOUNTS_KEY = "allAccounts";

@Controller
@RequestMapping("/")
class HomeController(
    private val userService: UserService,
    private val accountService: AccountService,
    private val transferService: TransferService
) {

    private val logger = KotlinLogging.logger { }

    @GetMapping
    fun get(@RequestParam(required = false) accID: String?): ModelAndView {
        val mav = ModelAndView("home")
        val currentUser =
            userService.loggedInUser!! // We rely on our security setup to ensure there is a logged in user at this point

        logger.debug { "Retrieving accounts for user $currentUser" }
        val accounts = accountService.findByOwner(currentUser.username)
        mav.addObject(ACCOUNT_LIST_KEY, accounts)

        logger.debug("Retrieving balances for ${accounts.size} accounts owned by user ${currentUser.username}")

        val balances = accounts.associateBy { it.accountID }.mapValues { accountService.getBalance(it.key) }
        mav.addObject(BALANCES_KEY, balances)

        /**
         * If an accountID is provided as a GET parameter (accID), try to find it in the list of own accounts.
         * If it is present, use that as the selected account. If not, use the first one in the list
         */
        val selectedAccountID = when (accID) {
            null -> accounts.first().accountID
            else -> accounts.firstOrNull { it.accountID == accID }?.accountID ?: accounts.first().accountID
        }
        mav.addObject(SELECTED_ACCOUNT_ID_KEY, selectedAccountID)

        logger.debug("Retrieving all transfers for selected account $selectedAccountID")
        val transfers = transferService.findTransfersByAccountID(selectedAccountID)
        mav.addObject(transfers)

        /**
         * Add all accounts to fill the address book in the UI
         */
        val allAccounts = accountService.findAllAccounts()
        mav.addObject(ALL_ACCOUNTS_KEY, allAccounts.associateBy { it.accountID })

        return mav

    }

}