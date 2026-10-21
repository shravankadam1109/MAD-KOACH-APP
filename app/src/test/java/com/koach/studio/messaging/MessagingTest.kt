package com.koach.studio.messaging

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.NotificationType
import com.koach.studio.messaging.data.InMemoryMessageRepository
import com.koach.studio.messaging.data.MessageRules
import com.koach.studio.messaging.ui.ConversationViewModel
import com.koach.studio.notifications.data.InMemoryNotificationRepository
import com.koach.studio.notifications.ui.NotificationsViewModel
import com.koach.studio.projects.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MessagingTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 10, 22)
    private val clock = Clock.fixed(today.atTime(14, 0).atZone(zone).toInstant(), zone)
    private val data = DemoData(today)
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }
    private val notifications = InMemoryNotificationRepository(data.notifications, auth.currentUser, access)
    private val messages = InMemoryMessageRepository(data.threads, data.messages, auth.currentUser, access, notifications, clock)

    @Test
    fun clients_onlySeeTheirThreads_andAgencySeesAll() = runTest {
        auth.signInAsDemo("u-maya")
        assertEquals(listOf("th-1"), messages.observeThreads().first().map { it.thread.id })
        assertTrue(messages.observeMessages("th-2").first().isEmpty())
        assertTrue(messages.sendMessage("th-2", "Hi").exceptionOrNull() is PermissionDeniedException)

        auth.signInAsDemo("u-rohan")
        assertEquals(3, messages.observeThreads().first().size)
    }

    @Test
    fun unreadCounts_ignoreOwnMessages_andClearWhenRead() = runTest {
        auth.signInAsDemo("u-maya")
        assertEquals(1, messages.observeUnreadCount().first()) // m-4 from Zoya; m-3 is already read
        messages.markThreadRead("th-1")
        assertEquals(0, messages.observeUnreadCount().first())
    }

    @Test
    fun send_validates_appends_andNotifiesOtherParticipants() = runTest {
        auth.signInAsDemo("u-maya")
        assertTrue(messages.sendMessage("th-1", "   ").isFailure)
        assertTrue(messages.sendMessage("th-1", "x".repeat(MessageRules.MAX_LENGTH + 1)).isFailure)

        val sent = messages.sendMessage("th-1", "Love the B-roll, use take 3!").getOrThrow()
        assertEquals(sent.id, messages.observeMessages("th-1").first().last().id)

        auth.signInAsDemo("u-zoya")
        val latest = notifications.observeNotifications().first().first()
        assertEquals(NotificationType.MESSAGE, latest.type)
        assertEquals("th-1", latest.targetId)
        assertFalse(latest.isRead)
    }

    @Test
    fun conversation_sendsDraft_andClearsIt() = runTest {
        auth.signInAsDemo("u-maya")
        val vm = ConversationViewModel("th-1", auth.currentUser.value!!, messages, data.users)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val before = vm.uiState.first { !it.isLoading }.messages.size
        assertFalse(vm.uiState.value.canSend)

        vm.onDraftChange("See you at the grade session")
        assertTrue(vm.uiState.value.canSend)
        vm.send()
        assertEquals(before + 1, vm.uiState.value.messages.size)
        assertEquals("", vm.uiState.value.draft)
    }

    @Test
    fun notificationCentre_groupsByDay_andMarksRead() = runTest {
        auth.signInAsDemo("u-maya")
        val vm = NotificationsViewModel(notifications, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }
        assertEquals(listOf("n-2"), state.today.map { it.id })
        assertEquals(2, state.unreadCount)

        vm.open(state.today.first())
        assertEquals(1, vm.uiState.value.unreadCount)
        vm.markAllRead()
        assertEquals(0, vm.uiState.value.unreadCount)
    }
}
