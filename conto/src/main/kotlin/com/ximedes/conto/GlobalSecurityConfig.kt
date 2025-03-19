package com.ximedes.conto

import com.ximedes.conto.core.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * This class provides global security configuration. It sets up
 * the user store and password encoding.
 * <p>
 * Channel-specific security (for web and REST API) are setup in
 * {@link com.ximedes.conto.web.security.WebSecurityConfig} and {@link com.ximedes.conto.api.security.APISecurityConfig}
 * respectively.
 */
@EnableWebSecurity(debug = false)
@EnableGlobalMethodSecurity(prePostEnabled = true)
class GlobalSecurityConfig {

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    /**
     * Here we configure Spring Security to use our userService.
     * Note that the @Lazy is here to work around a cyclic dependency
     * between this class and [UserService]
     *
     * @param auth
     * @throws Exception
     */
    @Autowired
    fun configureGlobal(@Lazy userService: UserService, auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(userService)
    }

}