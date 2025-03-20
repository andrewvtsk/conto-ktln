package com.ximedes.conto.core.service

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.ximedes.conto.core.domain.Account
import com.ximedes.conto.core.domain.Role
import com.ximedes.conto.core.domain.User
import com.ximedes.conto.core.port.output.AccountPort
import com.ximedes.conto.core.port.output.UserPort
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.verify
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User as SpringUser

class AccountServiceTest {

    private val accountPort = mock<AccountPort>()
    private val userPort = mock<UserPort>()
    private val accountService = AccountService(accountPort, userPort)

    private val accountCaptor = argumentCaptor<Account>()
    private lateinit var savedContext: SecurityContext

    @BeforeEach
    fun setup() {
        savedContext = SecurityContextHolder.getContext()
    }

    @AfterEach
    fun restoreContext() {
        SecurityContextHolder.setContext(savedContext)
    }

    @Test
    fun `a new account for a regular user has the correct properties`() {
        val user = User(username = "user", password = "pass", role = Role.USER)

        // Создаём Spring UserDetails с правами
        val userDetails = SpringUser(
            user.username,
            user.password,
            listOf(SimpleGrantedAuthority(user.role.authority))
        )

        // Устанавливаем контекст безопасности
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.authorities
        )

        whenever(userPort.findByUsername(user.username)).thenReturn(user)

        val account = accountService.createAccount("description")

        assertEquals("description", account.description)
        assertEquals(user.username, account.owner)
        assertEquals(0, account.minimumBalance)
        assertEquals(0L, account.balance) // Баланс теперь 0, а не null
        verify(accountPort).save(accountCaptor.capture())
        assertEquals(account, accountCaptor.firstValue)
    }

    @Test
    fun `a new account created by an admin has the correct properties`() {
        val owner = User(username = "user", password = "pass", role = Role.USER)
        val admin = User(username = "admin", password = "adminpass", role = Role.ADMIN)

        // Создаём UserDetails для админа
        val adminDetails = SpringUser(
            admin.username,
            admin.password,
            listOf(SimpleGrantedAuthority(admin.role.authority))
        )

        // Устанавливаем контекст безопасности
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            adminDetails, null, adminDetails.authorities
        )

        whenever(userPort.findByUsername(owner.username)).thenReturn(owner)

        val account = accountService.createAccount(owner.username, "description", -10000)

        assertEquals("description", account.description)
        assertEquals(owner.username, account.owner)
        assertEquals(-10000, account.minimumBalance)
        assertEquals(0L, account.balance) // Баланс теперь 0
        verify(accountPort).save(accountCaptor.capture())
        assertEquals(account, accountCaptor.firstValue)
    }

    @Test
    fun `findByOwner calls accountPort correctly`() {
        val username = "user"
        accountService.findByOwner(username)
        verify(accountPort).findByOwner(username)
    }
}
