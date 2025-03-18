package com.ximedes.conto.security

import com.ximedes.conto.core.port.output.AccountBalancePort
import com.ximedes.conto.service.UserService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.security.core.userdetails.User
import javax.security.auth.login.AccountNotFoundException


@Component("accountSecurity")
class AccountSecurity(
    private val accountBalancePort: AccountBalancePort,
    private val userService: UserService
) {

    fun hasAccessToAccount(accountID: String): Boolean {
        val currentUser = SecurityContextHolder.getContext().authentication.principal as User

        if (currentUser.authorities.any { it.authority == "ROLE_ADMIN" }) {
            return true
        }

        val accountOwner = accountBalancePort.getAccountOwner(accountID)
            ?: throw AccountNotFoundException("Account with ID $accountID not found.")

        return accountOwner == currentUser.username
    }
}
