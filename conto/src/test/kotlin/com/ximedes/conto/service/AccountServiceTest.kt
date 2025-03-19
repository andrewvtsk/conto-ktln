package com.ximedes.conto.core.service

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.ximedes.conto.AccountBuilder
import com.ximedes.conto.UserBuilder
import com.ximedes.conto.db.AccountMapper
import com.ximedes.conto.core.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher

class AccountServiceTest {

    val accountMapper = mock<AccountMapper>()
    val userService = mock<UserService>()
    val publisher = mock<ApplicationEventPublisher>()

    val accountService = AccountService(accountMapper, userService, publisher)

    val criteriaCaptor = argumentCaptor<AccountCriteria>()
    val accountCaptor = argumentCaptor<Account>()

    @Test
    fun `a new account for a regular user has the correct properties`() {
        val user = UserBuilder.build()
        val desc = "description"
        whenever(userService.loggedInUser).thenReturn(user)
        whenever(userService.findByUsername(user.getUsername())).thenReturn(user)

        val account = accountService.createAccount("description")
        assertEquals(desc, account.description)
        assertEquals(user.getUsername(), account.owner)
        assertEquals(0, account.minimumBalance)
        assertNull(account.balance)
        verify(accountMapper).insertAccount(account)
    }

    @Test
    fun `a new account created by an admin has the correct properties`() {
        val owner = UserBuilder.build()
        val admin = UserBuilder.build {
            username = "admin"
            role = Role.ADMIN
        }
        val desc = "description"

        whenever(userService.loggedInUser).thenReturn(admin)
        whenever(userService.findByUsername(owner.getUsername())).thenReturn(owner)

        val account = accountService.createAccount(owner.getUsername(), desc, -10000)
        assertEquals(desc, account.description)
        assertEquals(owner.getUsername(), account.owner)
        assertEquals(-10000, account.minimumBalance)
        assertNull(account.balance)
        verify(accountMapper).insertAccount(account)
    }

    @Test
    fun `finding all accounts means using an empty criteria object`() {
        accountService.findAllAccounts()
        verify(accountMapper).find(criteriaCaptor.capture())
        val criteria = criteriaCaptor.firstValue
        assertNull(criteria.accountID)
        assertNull(criteria.ownerID)
    }

    @Test
    fun `a checking account is created when a UserSignedUpEvent is received`() {
        accountService.onUserSignedUp(UserSignedUpEvent(this, "user"))
        verify(accountMapper).insertAccount(accountCaptor.capture())
        val a = accountCaptor.firstValue
        assertEquals("user", a.owner)
        assertEquals(0L, a.minimumBalance)
    }

    @Test
    fun `a root account is created when the AdminUserCreatedEvent is received`() {
        assertThrows(UninitializedPropertyAccessException::class.java) {
            accountService.rootAccount
        }

        accountService.onAdminUserCreated(AdminUserCreatedEvent(this, "iamadmin"))
        val bankAccount = accountService.rootAccount
        assertNotNull(bankAccount)
        assertEquals("iamadmin", bankAccount.owner)
        assertEquals(Long.MIN_VALUE, bankAccount.minimumBalance)
        assertEquals(null, bankAccount.balance)
    }

    @Test
    fun `findByOwner uses criteria in the right way`() {
        accountService.findByOwner("foo")
        verify(accountMapper).find(criteriaCaptor.capture())
        assertEquals("foo", criteriaCaptor.firstValue.ownerID)
        assertNull(criteriaCaptor.firstValue.accountID)
    }

    @Test
    fun `find by id returns null when no account found`() {
        whenever(accountMapper.find(criteriaCaptor.capture())).thenReturn(emptyList())
        assertNull(accountService.findByAccountID("1234"))
        assertEquals("1234", criteriaCaptor.firstValue.accountID)
        assertNull(criteriaCaptor.firstValue.ownerID)
    }

    @Test
    fun `find by account ID returns first element if multiple found`() {
        val accountList = AccountBuilder.build(4)
        whenever(accountMapper.find(criteriaCaptor.capture())).thenReturn(accountList)
        assertSame(accountList.first(), accountService.findByAccountID("whatever"))
    }

    @Test
    fun `setAccountBalance uses accountId and balance in the right way`() {
        accountService.setAccountBalance("NLBRAT00075566", 140L)
        verify(accountMapper).setAccountBalance("NLBRAT00075566", 140L)
    }

    @Test
    fun `updateAccountBalanceWhenTransfer uses accountId and balance in the right way`() {
        accountService.updateAccountBalanceWhenTransfer("NLBRAT00075566", 140L)
        verify(accountMapper).updateAccountBalanceWhenTransfer("NLBRAT00075566", 140L)
    }

}