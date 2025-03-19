package com.ximedes.conto.adapter.output

import com.ximedes.conto.core.port.output.UserPort
import com.ximedes.conto.db.UserMapper
import com.ximedes.conto.core.domain.User
import org.springframework.stereotype.Repository

@Repository
class UserAdapter(private val userMapper: UserMapper) : UserPort {

    override fun findByUsername(username: String): User? {
        return userMapper.findByUsername(username)
    }

    override fun insertUser(user: User, canonicalUsername: String) {
        userMapper.insertUser(user, canonicalUsername)
    }

    override fun isCommonPassword(password: String): Boolean {
        return userMapper.isCommonPassword(password)
    }
}
