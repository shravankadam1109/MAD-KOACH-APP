package com.koach.studio.auth

import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.Role
import com.koach.studio.projects.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val users = DemoData(LocalDate.of(2026, 10, 23)).users
    private val auth = DemoAuthRepository(users)

    @Test
    fun validator_checksEmailFormatAndPasswordLength() {
        assertEquals(setOf(LoginValidator.Field.EMAIL, LoginValidator.Field.PASSWORD), LoginValidator.validate("", "").keys)
        assertEquals(setOf(LoginValidator.Field.EMAIL), LoginValidator.validate("maya@", "secret1").keys)
        assertEquals(setOf(LoginValidator.Field.PASSWORD), LoginValidator.validate("maya@aetheria.example", "123").keys)
        assertTrue(LoginValidator.validate("maya@aetheria.example", "secret1").isEmpty())
    }

    @Test
    fun eachPortal_offersOnlyItsOwnDemoAccounts() {
        val client = LoginViewModel(AuthPortal.CLIENT, auth)
        val team = LoginViewModel(AuthPortal.AGENCY, auth)
        assertTrue(client.uiState.value.demoAccounts.all { it.role == Role.CLIENT })
        assertTrue(team.uiState.value.demoAccounts.all { it.role.isAgency })
        assertTrue(client.uiState.value.demoAccounts.isNotEmpty() && team.uiState.value.demoAccounts.isNotEmpty())
    }

    @Test
    fun invalidForm_showsErrors_andDoesNotSignIn() = runTest {
        val vm = LoginViewModel(AuthPortal.CLIENT, auth)
        vm.onEmailChange("not-an-email")
        vm.signIn()
        assertTrue(LoginValidator.Field.EMAIL in vm.uiState.value.errors)
        assertTrue(LoginValidator.Field.PASSWORD in vm.uiState.value.errors)
        assertNull(auth.currentUser.value)
    }

    @Test
    fun clientDemoAccount_signsInThroughClientPortal() = runTest {
        val vm = LoginViewModel(AuthPortal.CLIENT, auth)
        vm.signInWithDemo("u-maya")
        assertTrue(vm.uiState.value.signedIn)
        assertEquals("u-maya", auth.currentUser.value?.id)
    }

    @Test
    fun agencyAccount_isNotOfferedOrAcceptedOnClientPortal() = runTest {
        val vm = LoginViewModel(AuthPortal.CLIENT, auth)
        vm.signInWithDemo("u-arjun")
        assertNull(auth.currentUser.value)
        assertFalse(vm.uiState.value.signedIn)
    }

    @Test
    fun togglePassword_flipsVisibility() {
        val vm = LoginViewModel(AuthPortal.AGENCY, auth)
        assertFalse(vm.uiState.value.passwordVisible)
        vm.togglePasswordVisibility()
        assertTrue(vm.uiState.value.passwordVisible)
        assertNotNull(vm.uiState.value.demoAccounts.firstOrNull { it.id == "u-zoya" })
    }
}
