package com.ximedes.conto.api.security

import com.ximedes.conto.core.service.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

private const val API_PATH_PARAM = "/api/**"

/**
 * This bean provides the security configuration for the Rest API.
 *
 * The [Order] annotation is here to solve an issue with having two beans that extend
 * [WebSecurityConfigurerAdapter] in our context (the other one being
 * [com.ximedes.conto.web.security.WebSecurityConfig]).
 *
 * WebSecurityConfigurerAdapter has
 * an @Order with a value of 100, and Spring does not allow two beans with the same
 * value for Order. So, this bean uses 99 to prevent this (it could have been any value).
 */
@Configuration
@Order(99)
class APISecurityConfig(
    private val tokenizer: JWTUserTokenizer,
    private val userService: UserService
) : WebSecurityConfigurerAdapter() {

    /**
     * The [JWTSecurityContextRepository] is responsible for
     * storing and retrieving JWTs in and from HTTP headers.
     */
    @Bean
    fun jwtSecurityContextRepository() = JWTSecurityContextRepository(tokenizer, userService)

    override fun configure(http: HttpSecurity) {
        http
            // This setup only applies to URLs that start with /api, so not the web frontend
            .antMatcher(API_PATH_PARAM)
            // Enable cors, automatically using the configuration from corsConfigurationSource
            .cors().and()
            // Use our JWTSecurityContextRepository
            .securityContext().securityContextRepository(jwtSecurityContextRepository())
            .and()
            // Do not create HTTP sessions ever, our API is completely stateless
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            /*
                We disable Spring's default CSRF support, mainly because it relies on server-side state
                (by storing the CSRF token value in the session between requests), and we want our API
                to be stateless. Instead, we rely on our signed JWT as sufficient protection against CSRF, see
                https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet#Encrypted_Token_Pattern
            */
            .csrf().disable()
            /*
                By default, Spring Security returns HTTP status 403 (Forbidden), but we want the more suited
                HTTP 401 (Unauthorized).
            */
            .exceptionHandling().authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)).and()
            .authorizeRequests()
            /*
                Always allow OPTIONS request, which are part of the CORS procedure.
                These requests do not include our authorization header with the JWT,
                so we need to always allow them explicitly
            */
            .antMatchers(HttpMethod.OPTIONS, API_PATH_PARAM).permitAll()
            // Obviously, the login url needs to be reachable by non-logged in users
            .antMatchers("/api/login").permitAll()
            // And the CSP report violations
            .antMatchers("/api/csp-report").permitAll()
            // Secure everything else
            .antMatchers(API_PATH_PARAM).authenticated()


        /*
         * The APIAuthenticationFilter is responsible for validating
         * username and password combinations when provided through the API
         * (It is essentially the API version of a login form, listening on
         * /api/login.)
         */
        http.addFilterBefore(
            APIAuthenticationFilter(authenticationManager()),
            UsernamePasswordAuthenticationFilter::class.java
        )
    }

    /**
     * The default CORS configuration is not suited for our purposes.
     *
     * First, the default allows requests from all origins, but for this demo
     * application we can limit it to `http://localhost:3000`, the origin
     * of our demo phone app.
     *
     * By default, only the GET and HEAD HTTP methods are allowed. We don't need HEAD,
     * but we do need POST and OPTIONS (for pre-flight requests).
     *
     * Finally, we need to allow the HTTP header in HTTP requests
     * containing our JWT to be passed to our application, and also the same header
     * in responses sent to the client to be available to the JavaScript application
     * running in the browser.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource? {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000")
            allowedMethods = listOf("GET", "POST", "OPTIONS")
            allowedHeaders = listOf("Content-Type", API_TOKEN_HEADER)
            exposedHeaders = listOf(API_TOKEN_HEADER)
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(API_PATH_PARAM, config)
        }

    }
}