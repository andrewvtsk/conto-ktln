package com.ximedes.conto.service
import com.ximedes.conto.db.TransferMapper
import com.ximedes.conto.core.port.output.AccountBalancePort
import com.ximedes.conto.core.port.output.TransferRepository
import com.ximedes.conto.core.port.input.TransferUseCase

import com.ximedes.conto.domain.*
import com.ximedes.conto.domain.AccountNotAvailableException.Type.*
import mu.KotlinLogging
// import org.springframework.context.event.EventListener
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

const val SIGNUP_BONUS = 100L

@Service
@Transactional
class TransferService(
    private val accountService: AccountService,
    private val accountBalancePort: AccountBalancePort,
    private val transferRepository: TransferRepository
): TransferUseCase {

    private val logger = KotlinLogging.logger { }

    @PreAuthorize("isAuthenticated()")
    override fun attemptTransfer(
        debitAccountID: String,
        creditAccountID: String,
        amount: Long,
        description: String
    ): Transfer {
        val debitBalance = accountBalancePort.getBalance(debitAccountID)

        if (debitBalance < amount) {
            throw InsufficientFundsException("Insufficient funds for transferring $amount")
        }

        val transfer = Transfer(debitAccountID, creditAccountID, amount, description)

        if (!accountBalancePort.updateBalance(debitAccountID, -amount)) {
            throw RuntimeException("Transfer failed due to concurrent modification")
        }
        if (!accountBalancePort.updateBalance(creditAccountID, amount)) {
            throw RuntimeException("Transfer failed due to concurrent modification")
        }

        transferRepository.saveTransfer(transfer)

        return transfer
    }

    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.hasAccessToAccount(#accountID)")
    override fun findTransfersByAccountID(accountID: String): List<Transfer> {
        return transferRepository.findTransfersByAccountID(accountID)
    }

    override fun grantSignupBonus(accountID: String) {
        val rootAccount = accountService.getRootAccount()
            ?: throw IllegalStateException("Cannot grant signup bonus because root account is not initialized")
    
        logger.info("Granting signup bonus from ${rootAccount.accountID} to new account $accountID")
    
        val transfer = Transfer(rootAccount.accountID, accountID, SIGNUP_BONUS, "Welcome to Conto!")
    
        transferRepository.saveTransfer(transfer)
        accountBalancePort.updateBalance(accountID, SIGNUP_BONUS)
    }

}
