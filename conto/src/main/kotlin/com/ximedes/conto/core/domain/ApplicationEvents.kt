package com.ximedes.conto.core.domain

import org.springframework.context.ApplicationEvent

class AdminUserCreatedEvent(source: Any, val adminUsername: String) : ApplicationEvent(source)

class UserSignedUpEvent(source: Any, val username: String) : ApplicationEvent(source)

class FirstAccountCreatedEvent(source: Any, val owner: String, val accountID: String) : ApplicationEvent(source)