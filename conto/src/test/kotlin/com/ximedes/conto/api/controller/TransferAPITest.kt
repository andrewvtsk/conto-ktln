package com.ximedes.conto.api.controller

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.ximedes.conto.TransferBuilder
import com.ximedes.conto.TransferRequestBuilder
import com.ximedes.conto.core.domain.AccountNotAvailableException
import com.ximedes.conto.core.domain.AccountNotAvailableException.Type.DEBIT
import com.ximedes.conto.core.domain.InsufficientFundsException
import com.ximedes.conto.core.service.TransferService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult


class TransferAPITest {
    private val transferService = mock<TransferService>()
    private val bindingResult = mock<BindingResult>()
    val api = TransferAPI(transferService)

    @Test
    fun `message is properly mapped to service call`() {
        val transferRequest = TransferRequestBuilder.build()
        val transfer = TransferBuilder.build { fromTransferRequest(transferRequest) }
        whenever(
            transferService.attemptTransfer(
                transferRequest.debitAccountID,
                transferRequest.creditAccountID,
                transferRequest.amount,
                transferRequest.description
            )
        ).thenReturn(transfer)

        val response = api.createTransfer(transferRequest, bindingResult)
        verify(transferService).attemptTransfer(
            transferRequest.debitAccountID,
            transferRequest.creditAccountID,
            transferRequest.amount,
            transferRequest.description
        )

        val transferDTO = (response.body as TransferResponse).transfer!!
        assertAll(
            { assertEquals(transfer.debitAccountID, transferDTO.debitAccountID) },
            { assertEquals(transfer.creditAccountID, transferDTO.creditAccountID) },
            { assertEquals(transfer.amount, transferDTO.amount) },
            { assertEquals(transfer.description, transferDTO.description) },
            { assertEquals(transfer.transferID, transferDTO.transferID) }
        )
    }

    @Test
    fun testTransferByAccountID() {
        val accountID = "123"
        val t = TransferBuilder.build { debitAccountID = accountID }
        whenever(transferService.findTransfersByAccountID(accountID)).thenReturn(listOf(t))

        val jt = api.getTransfersByAccountID(accountID).body?.single() ?: fail("Should not be null")

        assertAll(
            { assertEquals(t.debitAccountID, jt.debitAccountID) },
            { assertEquals(t.creditAccountID, jt.creditAccountID) },
            { assertEquals(t.amount, jt.amount) },
            { assertEquals(t.description, jt.description) },
            { assertEquals(t.transferID, jt.transferID) }
        )
    }

    @Test
    fun `it throws an exception when insufficient funds for transferring`() {

        val transferRequest = TransferRequestBuilder.build()

        whenever(
            transferService.attemptTransfer(
                transferRequest.debitAccountID,
                transferRequest.creditAccountID,
                transferRequest.amount,
                transferRequest.description
            )
        ).then {
            throw InsufficientFundsException(("Insufficient funds for transferring ${transferRequest.amount} from account ${transferRequest.debitAccountID} with balance 0"))
        }

        val response = api.createTransfer(transferRequest, bindingResult)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Insufficient funds", response.body?.errors?.get(0))
    }

    @Test
    fun `it throws an exception when account not available`() {

        val transferRequest = TransferRequestBuilder.build()

        whenever(
            transferService.attemptTransfer(
                transferRequest.debitAccountID,
                transferRequest.creditAccountID,
                transferRequest.amount,
                transferRequest.description
            )
        ).then {
            throw AccountNotAvailableException(
                DEBIT,
                "Debit account with ID $transferRequest.debitAccountID not found."
            )
        }

        val response = api.createTransfer(transferRequest, bindingResult)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(
            "Debit account with ID TransferRequest(debitAccountID=debit, creditAccountID=credit, amount=0, description=description).debitAccountID not found.",
            response.body?.errors?.get(0)
        )
    }

}