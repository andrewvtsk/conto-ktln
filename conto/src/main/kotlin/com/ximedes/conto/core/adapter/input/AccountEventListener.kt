package com.ximedes.conto.adapter.input

import com.ximedes.conto.core.port.input.TransferUseCase
import com.ximedes.conto.domain.FirstAccountCreatedEvent
import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AccountEventListener(private val transferUseCase: TransferUseCase) {

    private val logger = KotlinLogging.logger {}

    @EventListener
    fun onFirstAccountCreated(event: FirstAccountCreatedEvent) {
        logger.info { "Processing signup bonus for ${event.accountID}" }
        transferUseCase.grantSignupBonus(event.accountID)
    }
}
