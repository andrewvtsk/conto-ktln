package com.ximedes.conto.adapter.input

import com.ximedes.conto.core.port.input.AccountUseCase
import com.ximedes.conto.core.port.input.TransferUseCase
import com.ximedes.conto.core.domain.FirstAccountCreatedEvent
import com.ximedes.conto.core.domain.AdminUserCreatedEvent
import com.ximedes.conto.core.domain.UserSignedUpEvent
import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AccountEventListener(private val transferUseCase: TransferUseCase, private val accountUseCase: AccountUseCase) {

    private val logger = KotlinLogging.logger {}

    @EventListener
    fun onFirstAccountCreated(event: FirstAccountCreatedEvent) {
        logger.info { "Processing signup bonus for ${event.accountID}" }
        transferUseCase.grantSignupBonus(event.accountID)
    }

    @EventListener
    fun onAdminUserCreated(event: AdminUserCreatedEvent) {
        logger.info { "Creating system bank account for admin ${event.adminUsername}" }
        accountUseCase.createAccount(event.adminUsername, "Bank", Long.MIN_VALUE)
    }

    @EventListener
    fun onUserSignedUp(event: UserSignedUpEvent) {
        logger.info { "Creating first account for user ${event.username}" }
        val account = accountUseCase.createAccount(event.username, "Checking", 0L)
    }
}
