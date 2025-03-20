package com.ximedes.conto.core.service

import com.nhaarman.mockitokotlin2.*
import com.ximedes.conto.core.port.output.UserPort
import com.ximedes.conto.core.domain.AdminUserCreatedEvent
import com.ximedes.conto.core.domain.Role
import com.ximedes.conto.core.domain.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest {

    private val userPort = mock<UserPort>()
    private val passwordEncoder = mock<PasswordEncoder>()
    private val eventPublisher = mock<ApplicationEventPublisher>()

    private val userService = UserService(userPort, passwordEncoder, eventPublisher)
    private val userCaptor = argumentCaptor<User>()
    private val eventCaptor = argumentCaptor<ApplicationEvent>()

    private lateinit var savedContext: SecurityContext

    @BeforeEach
    fun setup() {
        savedContext = SecurityContextHolder.getContext()
    }

    @AfterEach
    fun restoreContext() = SecurityContextHolder.setContext(savedContext)

    @Test
    fun `Admin user is created after context refresh event`() {
        whenever(passwordEncoder.encode(any())).thenReturn("encodedpassword")

        userService.onContextRefreshedEvent(null)

        verify(userPort).insertUser(userCaptor.capture(), check { assertEquals("admin", it) })
        val admin = userCaptor.firstValue
        assertEquals("encodedpassword", admin.password)
        assertEquals(Role.ADMIN, admin.role)
        assertEquals(ADMIN_USERNAME, admin.username)

        verify(eventPublisher).publishEvent(eventCaptor.capture())
        val event = eventCaptor.firstValue as AdminUserCreatedEvent
        assertEquals(admin.username, event.adminUsername)
    }

    @Test
    fun `Finding a user by username returns the correct user`() {
        val testUser = User("testUser", "hashedPassword", Role.USER)
        whenever(userPort.findByUsername("testUser")).thenReturn(testUser)

        val foundUser = userService.findByUsername("testUser")

        assertEquals(testUser, foundUser)
        verify(userPort).findByUsername("testUser")
    }

    @Test
    fun `Password encoding works correctly`() {
        whenever(passwordEncoder.encode("testPassword")).thenReturn("encodedPassword")

        val encodedPassword = passwordEncoder.encode("testPassword")

        assertEquals("encodedPassword", encodedPassword)
        verify(passwordEncoder).encode("testPassword")
    }
}
