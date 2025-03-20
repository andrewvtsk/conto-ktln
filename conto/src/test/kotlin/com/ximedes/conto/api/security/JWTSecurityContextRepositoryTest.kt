package com.ximedes.conto.api.security

import com.nhaarman.mockitokotlin2.*
import com.ximedes.conto.core.domain.Role
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.security.web.context.HttpRequestResponseHolder
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JWTSecurityContextRepositoryTest {

    private val tokenizer = mock<JWTUserTokenizer>()
    private val userDetailsService = mock<UserDetailsService>()
    private val repository = JWTSecurityContextRepository(tokenizer, userDetailsService)

    @Test
    fun `should load security context from JWT token`() {
        val username = "testuser"
        val request = mock<HttpServletRequest>()
        val response = mock<HttpServletResponse>()

        // Создаем SimpleGrantedAuthority вручную
        val userDetails = SpringUser(username, "password", listOf(SimpleGrantedAuthority(Role.USER.authority)))

        whenever(request.getHeader(API_TOKEN_HEADER)).thenReturn("valid_token")
        whenever(tokenizer.validateTokenAndExtractUsername("valid_token")).thenReturn(username)
        whenever(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails)

        val requestResponseHolder = HttpRequestResponseHolder(request, response)
        val context: SecurityContext = repository.loadContext(requestResponseHolder)

        assertNotNull(context.authentication)
        assertTrue(context.authentication is UsernamePasswordAuthenticationToken)
        assertEquals(username, context.authentication.name)
    }

    @Test
    fun `should return false if no JWT token is present`() {
        val request = mock<HttpServletRequest>()

        whenever(request.getHeader(API_TOKEN_HEADER)).thenReturn(null)

        val result = repository.containsContext(request)

        assertFalse(result)
    }

}
