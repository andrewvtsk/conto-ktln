package com.ximedes.conto.api.controller

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.ximedes.conto.service.AccountService
import com.ximedes.conto.service.TransferService
import com.ximedes.conto.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/account")
class AccountAPI(
    private val accountService: AccountService,
    private val transferService: TransferService,
    private val userService: UserService
) {

    @GetMapping
    fun findAccounts(): ResponseEntity<List<AccountDTO>> {
        val user = userService.loggedInUser?.username
        val response = accountService.findAllAccounts().map { a ->
            // Only add sensitive info if the current user is the owner of the account
            if (a.owner == user) {
                val accountBalance = accountService.getBalance(a.accountID)
    
                AccountDTO(a.accountID, a.owner, a.description, a.minimumBalance, accountBalance)
            } else {
                AccountDTO(a.accountID, a.owner, a.description)
            }
        }
        return ResponseEntity.ok(response)
    }

}

@JsonInclude(NON_NULL)
class AccountDTO(
    val accountID: String,
    val owner: String,
    val description: String,
    val minimumBalanceAllowed: Long? = null,
    val balance: Long? = null
)
