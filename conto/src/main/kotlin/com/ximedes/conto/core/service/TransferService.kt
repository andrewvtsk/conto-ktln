package com.ximedes.conto.core.service

import com.ximedes.conto.db.TransferMapper
import com.ximedes.conto.core.port.output.AccountBalancePort
import com.ximedes.conto.core.port.output.TransferRepository
import com.ximedes.conto.core.port.input.TransferUseCase

import com.ximedes.conto.core.domain.*
import com.ximedes.conto.core.domain.AccountNotAvailableException.Type.*
import mu.KotlinLogging
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
        /**
         * Note: it is simple solution for the case when all data in the same database. For the cases whenn it is necessary to 
         * meet consensus among different data bases, different services or separate threads the appropriate logic should be implemented.
         */
        val maxRetries = 3
        val transfer = Transfer(debitAccountID, creditAccountID, amount, description)

        logger.info("Started Transfer form $debitAccountID to $creditAccountID")

        retryOperation("Save Transfer", maxRetries) {
            transferRepository.saveTransfer(transfer)
        }

        retryOperation("Debit account", maxRetries) {
            if (!accountBalancePort.updateBalanceDebit(debitAccountID, amount)) {
                throw ConcurrentModificationException("Failed to update debit account balance")
            }
        }

        retryOperation("Credit account", maxRetries) {
            if (!accountBalancePort.updateBalanceCredit(creditAccountID, amount)) {
                throw ConcurrentModificationException("Failed to update credit account balance")
            }
        }

        logger.info("Completed transfer form $debitAccountID to $creditAccountID")

        return transfer
    }

    // Wrapper to implement retry mechanizm for db operations
    // Used Protected so that it is inaccessible for testing
    protected fun <T> retryOperation(operationName: String, maxRetries: Int, block: () -> T): T {
        var retryCount = 0
        while (retryCount < maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                retryCount++
                logger.warn { "Retrying $operationName (attempt $retryCount/$maxRetries) due to: ${e.message}" }
                if (retryCount == maxRetries) {
                    throw ConcurrentModificationException("$operationName failed after $maxRetries attempts")
                }
            }
        }
        throw ConcurrentModificationException("$operationName failed unexpectedly")
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
        accountBalancePort.updateBalanceCredit(accountID, SIGNUP_BONUS)
    }

}
