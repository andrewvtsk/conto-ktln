package com.ximedes.conto.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails

object SecurityUtils {
    fun getCurrentUser(): UserDetails {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.principal as UserDetails
    }
}
