package com.ximedes.conto.core.port.output

import com.ximedes.conto.core.domain.User

interface UserPort {
    fun findByUsername(username: String): User?
    fun insertUser(user: User, canonicalUsername: String)
    fun isCommonPassword(password: String): Boolean
}
