package com.ximedes.conto

import com.ximedes.conto.api.controller.TransferRequest
import com.ximedes.conto.core.domain.Account
import com.ximedes.conto.core.domain.Role
import com.ximedes.conto.core.domain.Transfer
import com.ximedes.conto.core.domain.User
import com.ximedes.conto.web.controller.AdminCreateAccountForm
import com.ximedes.conto.web.controller.CreateAccountForm
import com.ximedes.conto.web.controller.TransferForm

private var USER_ID = 1L

class UserBuilder {
    var username = "user-${USER_ID++}"
    var password = "password"
    var role = Role.USER

    private fun build(): User = User(username, password, role)

    companion object {
        fun build(config: UserBuilder.() -> Unit = {}) = UserBuilder().apply(config).build()
    }
}


class AccountBuilder {
    var accountID = "account-${ACCOUNT_ID++}"
    var owner = "owner-$ACCOUNT_ID"
    var description = "description-$ACCOUNT_ID"
    var minimumBalance = 0L
    var balance: Long? = null

    private fun build() = Account(accountID, owner, description, minimumBalance, balance)

    companion object {
        private var ACCOUNT_ID = 1L

        fun build(config: AccountBuilder.() -> Unit = {}): Account {
            return AccountBuilder().apply(config).build()
        }

        fun build(n: Int, config: AccountBuilder.(i: Int) -> Unit = {}) = (1..n).map {
            AccountBuilder().apply { config(it) }.build()
        }
    }
}


class TransferRequestBuilder {
    var debitAccountID = "debit"
    var creditAccountID = "credit"
    var amount = 0L
    var description = "description"

    private fun build() = TransferRequest(debitAccountID, creditAccountID, amount, description)

    companion object {
        fun build(config: TransferRequestBuilder.() -> Unit = {}) = TransferRequestBuilder().apply(config).build()
    }
}


class TransferBuilder {
    var transferID = TRANSFER_ID++
    var debitAccountID = "debit"
    var creditAccountID = "credit"
    var amount = 0L
    var description = "description"

    fun fromTransferRequest(trq: TransferRequest) {
        debitAccountID = trq.debitAccountID
        creditAccountID = trq.creditAccountID
        amount = trq.amount
        description = trq.description
    }

    private fun build() = Transfer(transferID, debitAccountID, creditAccountID, amount, description)

    companion object {
        private var TRANSFER_ID = 1L

        fun build(config: TransferBuilder.() -> Unit = {}) = TransferBuilder().apply(config).build()
        fun build(tf: TransferForm) = TransferBuilder().apply {
            debitAccountID = tf.fromAccountID!!
            creditAccountID = tf.toAccountID!!
            amount = tf.amount!!
            description = tf.description!!
        }.build()
    }
}

class AccountFormBuilder {
    var description: String? = "description"
    var minimumBalance: Long? = 0L
    var ownerID: String? = "ownerID"

    private fun build() = CreateAccountForm(description)

    private fun buildAdmin() = AdminCreateAccountForm(ownerID, minimumBalance, description)

    companion object {

        fun build(config: AccountFormBuilder.() -> Unit = {}): CreateAccountForm = AccountFormBuilder().apply(config).build()

        fun buildAdmin(config: AccountFormBuilder.() -> Unit = {}): AdminCreateAccountForm = AccountFormBuilder().apply(config).buildAdmin()

    }
}

class TransferFormBuilder {
    var fromAccountID: String? = "fromAccountID"
    var toAccountID: String? = "toAccountID"
    var amount: Long? = 123L
    var description: String? = "description"

    private fun build() = TransferForm(fromAccountID, toAccountID, amount, description)

    companion object {
        fun build(config: TransferFormBuilder.() -> Unit = {}) = TransferFormBuilder().apply(config).build()
    }
}