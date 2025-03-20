package com.ximedes.conto.core.service

import com.ximedes.conto.asCanonicalUsername
import com.ximedes.conto.core.port.output.UserPort
import com.ximedes.conto.core.domain.AdminUserCreatedEvent
import com.ximedes.conto.core.domain.Role
import com.ximedes.conto.core.domain.User
import com.ximedes.conto.core.domain.UserSignedUpEvent
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer

const val ADMIN_USERNAME = "admin"
private const val ADMIN_PASSWORD = "admin"

@Service
@Transactional
class UserService(
    private val userPort: UserPort,
    private val encoder: PasswordEncoder,
    private val eventPublisher: ApplicationEventPublisher
) : UserDetailsService {

    private val logger = KotlinLogging.logger { }

    val loggedInUser: User?
        get() = (SecurityContextHolder.getContext().authentication?.principal as? SpringUser)
            ?.let { findByUsername(it.username) }


    @EventListener
    fun onContextRefreshedEvent(e: ContextRefreshedEvent?) {
        logger.info("Creating admin user with username '$ADMIN_USERNAME' and default password")
        val admin = User(ADMIN_USERNAME, encoder.encode(ADMIN_PASSWORD), Role.ADMIN)
        userPort.insertUser(admin, ADMIN_USERNAME)
        
        authenticateUser(admin)

        eventPublisher.publishEvent(AdminUserCreatedEvent(this, ADMIN_USERNAME))
    }

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userPort.findByUsername(username) ?: throw UsernameNotFoundException(username)
        return createSpringUser(user)
    }

    fun findByUsername(username: String): User? {
        return userPort.findByUsername(username)
    }

    fun isCommonPassword(password: String?) = password?.let {
        userPort.isCommonPassword(Normalizer.normalize(password, Normalizer.Form.NFKC))
    } ?: true

    fun signupAndLogin(username: String, password: String): User {
        val user = User(username, encoder.encode(password), Role.USER)
        userPort.insertUser(user, username.asCanonicalUsername())
        authenticateUser(user)

        eventPublisher.publishEvent(UserSignedUpEvent(this, user.username))
        return user
    }

    private fun authenticateUser(user: User) {
        val authorities = setOf(SimpleGrantedAuthority(user.role.authority))
        val authToken = UsernamePasswordAuthenticationToken(
            SpringUser(user.username, user.password, authorities), user.password, authorities
        )
        SecurityContextHolder.getContext().authentication = authToken
    }

    private fun createSpringUser(user: User): SpringUser {
        return SpringUser(user.username, user.password, setOf(SimpleGrantedAuthority(user.role.authority)))
    }
}