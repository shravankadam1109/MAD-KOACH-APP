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

    @Test
    fun emailAndPassword_signInThroughMatchingPortal() = runTest {
        val vm = LoginViewModel(AuthPortal.AGENCY, auth)
        vm.onEmailChange("zoya@koach.example")
        vm.onPasswordChange(DemoAuthRepository.DEMO_PASSWORD)
        vm.signIn()
        assertTrue(vm.uiState.value.signedIn)
        assertEquals("u-zoya", auth.currentUser.value?.id)
    }

    @Test
    fun agencyCredentials_onClientPortal_areRejectedAndSignedOut() = runTest {
        val vm = LoginViewModel(AuthPortal.CLIENT, auth)
        vm.onEmailChange("arjun@koach.example")
        vm.onPasswordChange(DemoAuthRepository.DEMO_PASSWORD)
        vm.signIn()
        assertNull(auth.currentUser.value)
        assertTrue(vm.uiState.value.errorMessage.orEmpty().contains("Agency team"))
    }

    @Test
    fun wrongPassword_showsGenericError() = runTest {
        val vm = LoginViewModel(AuthPortal.CLIENT, auth)
        vm.onEmailChange("maya@aetheria.example")
        vm.onPasswordChange("wrongpass")
        vm.signIn()
        assertEquals("Email or password is incorrect", vm.uiState.value.errorMessage)
    }

    @Test
    fun forgotPassword_needsValidEmail_thenExplainsDemoLimit() = runTest {
        val vm = LoginViewModel(AuthPortal.CLIENT, auth)
        vm.requestPasswordReset()
        assertTrue(LoginValidator.Field.EMAIL in vm.uiState.value.errors)
        vm.onEmailChange("maya@aetheria.example")
        vm.requestPasswordReset()
        assertTrue(vm.uiState.value.infoMessage.orEmpty().contains("Firebase"))
        assertEquals(DemoAuthRepository.DEMO_PASSWORD, vm.uiState.value.demoPassword)
    }

    @Test
    fun signUpValidator_checksNamePhoneAndEmail() {
        val errors = LoginValidator.validateSignUp("", "bad-email", "123", "short")
        assertTrue(LoginValidator.Field.NAME in errors)
        assertTrue(LoginValidator.Field.EMAIL in errors)
        assertTrue(LoginValidator.Field.PHONE in errors)
        assertTrue(LoginValidator.Field.PASSWORD in errors)

        val valid = LoginValidator.validateSignUp("Aarav Roy", "aarav@example.com", "+91 9876543210", "securepass123")
        assertTrue(valid.isEmpty())
    }

    @Test
    fun signUpViewModel_registersNewClientAccount() = runTest {
        val vm = SignUpViewModel(auth)
        vm.onNameChange("Priya Sharma")
        vm.onEmailChange("priya@creative.example")
        vm.onPhoneChange("+91 98765 11111")
        vm.onPasswordChange("password123")
        vm.signUp()

        assertTrue(vm.uiState.value.signedUp)
        assertEquals("Priya Sharma", auth.currentUser.value?.name)
        assertEquals(Role.CLIENT, auth.currentUser.value?.role)
    }

    @Test
    fun googleSignIn_signsInClient() = runTest {
        val vm = LoginViewModel(AuthPortal.CLIENT, auth)
        vm.signInWithGoogle("mock-token")
        assertTrue(vm.uiState.value.signedIn)
        assertEquals(Role.CLIENT, auth.currentUser.value?.role)
    }

    @Test
    fun otpViewModel_sendAndVerifyFlow() = runTest {
        val vm = OtpViewModel(auth)
        vm.onPhoneChange("+91 98765 22222")
        vm.sendOtp()

        assertEquals(OtpStep.CODE_ENTRY, vm.uiState.value.step)
        assertNotNull(vm.uiState.value.verificationId)

        vm.onOtpChange("123456")
        vm.verifyOtp()

        assertTrue(vm.uiState.value.signedIn)
        assertEquals(Role.CLIENT, auth.currentUser.value?.role)
    }
}
