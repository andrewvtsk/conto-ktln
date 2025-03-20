package com.ximedes.conto.core.service

import com.ximedes.conto.core.domain.Account
import com.ximedes.conto.core.port.input.AccountUseCase
import com.ximedes.conto.core.port.output.AccountPort
import com.ximedes.conto.core.port.output.UserPort
import com.ximedes.conto.security.SecurityUtils.getCurrentUser
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val FIRST_ACCOUNT_DESCRIPTION = "Checking"

@Service
@Transactional
class AccountService(
    private val accountPort: AccountPort,
    private val userPort: UserPort
) : AccountUseCase {

    private val logger = KotlinLogging.logger {}

    override fun getRootAccount(): Account {
        return accountPort.findByOwner("admin").firstOrNull()
            ?: throw IllegalStateException("Root account not found in DB!")
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    override fun createAccount(description: String): Account {
        val username = getCurrentUser().username
        val a = doCreateAccount(username, description, 0L)
        logger.info { "New account for $username created" }
        return a
    }

    // @PreAuthorize("hasRole('ROLE_ADMIN')")
    override fun createAccount(ownerName: String, description: String, minimumBalance: Long): Account {
        val user = userPort.findByUsername(ownerName)
            ?: throw UsernameNotFoundException("User $ownerName not found!")

        return doCreateAccount(user.username, description, minimumBalance)
    }

    private fun doCreateAccount(owner: String, description: String, minimumBalance: Long): Account {
        val accountID = generateAccountID()
        val account = Account(accountID, owner, description, minimumBalance, 0L)

        accountPort.save(account)
        logger.info("Created new account $account.")
        
        return account
    }


    private fun generateAccountID(): String {
        val numericID = accountPort.getNextUniqueID()
        return "NLBRAT" + numericID.toString().padStart(8, '0')
    }

    override fun findByOwner(user: String): List<Account> {
        return accountPort.findByOwner(user)
    }

}