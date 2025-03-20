package com.ximedes.conto

import com.ximedes.conto.db.UserMapper
import com.ximedes.conto.core.domain.Role
import com.ximedes.conto.core.domain.User
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var userMapper: UserMapper

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    protected fun createUser(vararg usernames: String) {
        for (username in usernames) {
            var existingUser = userMapper.findByUsername(username)
            if (existingUser != null) {
                println("User $username already exists, skipping creation.")
                continue
            }
            val u = User(username, passwordEncoder.encode(username), Role.USER)
            userMapper.insertUser(u, username.asCanonicalUsername())
        }
    }


}
