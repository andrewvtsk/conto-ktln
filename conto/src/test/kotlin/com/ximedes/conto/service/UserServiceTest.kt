package com.ximedes.conto.core.service

import com.nhaarman.mockitokotlin2.*
import com.ximedes.conto.db.UserMapper
import com.ximedes.conto.core.domain.AdminUserCreatedEvent
import com.ximedes.conto.core.domain.Role
import com.ximedes.conto.core.domain.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest {

    val userMapper = mock<UserMapper>()
    val passwordEncoder = mock<PasswordEncoder>()
    val eventPublisher = mock<ApplicationEventPublisher>()

    val userService = UserService(userMapper, passwordEncoder, eventPublisher)
    val userCaptor = argumentCaptor<User>()
    val eventCaptor = argumentCaptor<ApplicationEvent>()


    lateinit var savedContext: SecurityContext

    @BeforeEach
    fun setup() {
        savedContext = SecurityContextHolder.getContext()
    }

    @AfterEach
    fun restoreContext() = SecurityContextHolder.setContext(savedContext)

    @Test
    fun testAdminUserCreatedAfterContextRefreshedEvent() {
        whenever(passwordEncoder.encode(any())).thenReturn("encodedpassword")

        userService.onContextRefreshedEvent(null)

        verify(userMapper).insertUser(userCaptor.capture(), check { assertEquals("admin", it) })
        val admin = userCaptor.firstValue
        assertEquals("encodedpassword", admin.password)
        assertEquals(Role.ADMIN, admin.role)
        assertEquals(ADMIN_USERNAME, admin.username)

        verify(eventPublisher).publishEvent(eventCaptor.capture())
        val event = eventCaptor.firstValue as AdminUserCreatedEvent
        assertEquals(admin.username, event.adminUsername)
    }
}