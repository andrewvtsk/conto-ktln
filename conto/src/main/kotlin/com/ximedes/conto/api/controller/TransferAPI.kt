package com.ximedes.conto.api.controller

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.ximedes.conto.core.domain.AccountNotAvailableException
import com.ximedes.conto.core.domain.InsufficientFundsException
import com.ximedes.conto.core.domain.Transfer
import com.ximedes.conto.core.service.TransferService
import mu.KotlinLogging
import org.hibernate.validator.constraints.CodePointLength
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.Pattern

@RestController
@RequestMapping("/api/transfer")
class TransferAPI(private val transferService: TransferService) {

    @GetMapping(value = ["/{accountID}"])
    fun getTransfersByAccountID(@PathVariable accountID: String): ResponseEntity<List<TransferDTO>> {
        val transfers: List<Transfer> = transferService.findTransfersByAccountID(accountID)
        val response = transfers.map {
            TransferDTO(
                it.transferID,
                it.debitAccountID,
                it.creditAccountID,
                it.description,
                it.amount
            )
        }
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createTransfer(
        @RequestBody @Valid ct: TransferRequest,
        bindingResult: BindingResult
    ): ResponseEntity<TransferResponse> {

        if (bindingResult.hasErrors()) {
            return errorResponse(bindingResult.allErrors.map { it.defaultMessage ?: "Unknown" })
        }

        return try {
            val t = transferService.attemptTransfer(
                ct.debitAccountID,
                ct.creditAccountID,
                ct.amount,
                ct.description
            )
            successResponse(t)
        } catch (e: InsufficientFundsException) {
            errorResponse(listOf("Insufficient funds"))
        } catch (e: AccountNotAvailableException) {
            errorResponse(listOf(e.message ?: "Account not available"))
        }

    }

    private fun successResponse(t: Transfer): ResponseEntity<TransferResponse> {
        return ResponseEntity.ok(
            TransferResponse(
                transfer = TransferDTO(
                    t.transferID,
                    t.debitAccountID,
                    t.creditAccountID,
                    t.description,
                    t.amount
                )
            )
        )
    }

    private fun errorResponse(errors: List<String>): ResponseEntity<TransferResponse> {
        return ResponseEntity.badRequest().body(TransferResponse(errors = errors))
    }

}

data class TransferDTO(
    val transferID: Long,
    val debitAccountID: String,
    val creditAccountID: String,
    val description: String,
    val amount: Long
)

data class TransferRequest(
    @get:CodePointLength(min = 1, max = 40)
    @get:Pattern(
        regexp = "[\\p{L}|\\p{N}-]*",
        message = "AccountID may only contain letters, numbers, and dashes"
    )
    val debitAccountID: String = "",

    @get:CodePointLength(min = 1, max = 40)
    @get:Pattern(
        regexp = "[\\p{L}|\\p{N}-]*",
        message = "AccountID may only contain letters, numbers, and dashes"
    )
    val creditAccountID: String = "",

    @get:Min(1)
    val amount: Long = 0L,

    @get:CodePointLength(
        min = 1,
        max = 512
    )
    @get:Pattern(
        regexp = "[\\p{L}|\\p{M}|\\p{N}|\\p{P}|\\p{Zs}]*",
        message = "Description may only contain letters, marks, numbers, punctuation and spaces"
    )
    val description: String = ""

)

@JsonInclude(NON_NULL)
data class TransferResponse(
    val transfer: TransferDTO? = null,
    val errors: List<String>? = null
)