package com.ximedes.conto.core.service

import com.nhaarman.mockitokotlin2.*
import com.ximedes.conto.core.domain.*
import com.ximedes.conto.core.port.output.AccountBalancePort
import com.ximedes.conto.core.port.output.TransferRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import java.util.ConcurrentModificationException

class TransferServiceTest {

    private val accountBalancePort = mock<AccountBalancePort>()
    private val transferRepository = mock<TransferRepository>()
    private val accountService = mock<AccountService>()

    private val transferService = TransferService(accountService, accountBalancePort, transferRepository)

    @BeforeEach
    fun setup() {
        reset(accountBalancePort, transferRepository, accountService)
    }

    @Test
    fun `transfer with sufficient balance succeeds`() {
        val debitAccountID = "NLBRAT00000001"
        val creditAccountID = "NLBRAT00000002"
        val amount = 100L
        val description = "Payment"
        val transfer = Transfer(debitAccountID, creditAccountID, amount, description)

        whenever(transferRepository.saveTransfer(any())).then { }
        whenever(accountBalancePort.updateBalanceDebit(debitAccountID, amount)).thenReturn(true)
        whenever(accountBalancePort.updateBalanceCredit(creditAccountID, amount)).thenReturn(true)

        val result = transferService.attemptTransfer(debitAccountID, creditAccountID, amount, description)

        assertEquals(transfer.debitAccountID, result.debitAccountID)
        assertEquals(transfer.creditAccountID, result.creditAccountID)
        assertEquals(transfer.amount, result.amount)
        assertEquals(transfer.description, result.description)

        verify(transferRepository).saveTransfer(any())
        verify(accountBalancePort).updateBalanceDebit(debitAccountID, amount)
        verify(accountBalancePort).updateBalanceCredit(creditAccountID, amount)
    }

    @Test
    fun `transfer fails when debit balance update fails`() {
        val debitAccountID = "NLBRAT00000001"
        val creditAccountID = "NLBRAT00000002"
        val amount = 100L
        val description = "Payment"
    
        whenever(transferRepository.saveTransfer(any())).then { }
        whenever(accountBalancePort.updateBalanceDebit(debitAccountID, amount)).thenReturn(false)
    
        val exception = assertThrows(RuntimeException::class.java) {
            transferService.attemptTransfer(debitAccountID, creditAccountID, amount, description)
        }
    
        assertEquals("Debit account failed after 3 attempts", exception.message)
    
        verify(transferRepository).saveTransfer(any())
        verify(accountBalancePort, times(3)).updateBalanceDebit(debitAccountID, amount)
        verify(accountBalancePort, never()).updateBalanceCredit(any(), any())
    }

    @Test
    fun `retryOperation retries on exception and eventually succeeds`() {
        val operationName = "Test Operation"
        var retryCount = 0

        val testableService = TestableTransferService(accountService, accountBalancePort, transferRepository)
        val result = testableService.testRetryOperation(operationName, 3) {
            if (retryCount++ < 2) throw ConcurrentModificationException("Transient failure")
            "Success"
        }

        assertEquals("Success", result)
    }

    @Test
    fun `retryOperation fails after max retries`() {
        val operationName = "Test Operation"

        val testableService = TestableTransferService(accountService, accountBalancePort, transferRepository)
        val exception = assertThrows(ConcurrentModificationException::class.java) {
            testableService.testRetryOperation(operationName, 3) {
                throw ConcurrentModificationException("Always failing")
            }
        }

        assertEquals("Test Operation failed after 3 attempts", exception.message)
    }
}

class TestableTransferService(
    accountService: AccountService,
    accountBalancePort: AccountBalancePort,
    transferRepository: TransferRepository
) : TransferService(accountService, accountBalancePort, transferRepository) {
    fun testRetryOperation(operationName: String, maxRetries: Int, block: () -> String): String {
        return retryOperation(operationName, maxRetries, block)
    }
}
