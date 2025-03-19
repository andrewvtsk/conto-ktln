package com.ximedes.conto.api.controller

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.ximedes.conto.core.port.output.AccountBalancePort
import com.ximedes.conto.core.port.input.AccountUseCase
import com.ximedes.conto.core.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/account")
class AccountAPI(
    private val accountService: AccountUseCase,
    private val accountBalancePort: AccountBalancePort,
    private val userService: UserService
) {

    @GetMapping
    fun findAccounts(): ResponseEntity<List<AccountDTO>> {
        val user = userService.loggedInUser?.username ?: return ResponseEntity.badRequest().build()
        val response = accountService.findByOwner(user).map { a ->
            val accountBalance = accountBalancePort.getBalance(a.accountID)

            AccountDTO(a.accountID, a.owner, a.description, a.minimumBalance, accountBalance)
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
