package com.ximedes.conto.db

import com.ximedes.conto.AbstractIntegrationTest
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.TransferBuilder
import com.ximedes.conto.core.domain.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TransferMapperTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var transferMapper: TransferMapper

    @Autowired
    lateinit var accountMapper: AccountMapper

    lateinit var debit: Account
    lateinit var credit: Account


    @BeforeAll
    fun setup() {
        createUser("debit-user", "credit-user")
        debit = AccountBuilder.build {
            owner = "debit-user"
        }.also { accountMapper.insertAccount(it) }

        credit = AccountBuilder.build {
            owner = "credit-user"
        }.also { accountMapper.insertAccount(it) }
    }

    @Test
    fun `basic insert and find transfer works`() {
        val t1 = TransferBuilder.build {
            transferID = -1L
            debitAccountID = debit.accountID
            creditAccountID = credit.accountID
        }
        transferMapper.insertTransfer(t1)
        assertNotEquals(-1L, t1.transferID)

        assertEquals(t1, transferMapper.findByTransferID(t1.transferID))
    }


}