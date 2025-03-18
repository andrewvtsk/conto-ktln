package com.ximedes.conto.domain

import org.springframework.security.core.GrantedAuthority

enum class Role : GrantedAuthority {
    ADMIN, USER;

    override fun getAuthority() = "ROLE_$name"
}

data class User(val username: String, val password: String, val role: Role)

val User?.isAdmin: Boolean
    get() = (this?.role == Role.ADMIN)

fun User?.hasAccessTo(account: Account): Boolean {
    val isOwner = account.owner == this?.username
    return isOwner || isAdmin
}