package com.rungo.api.domain.notification.listener

import com.rungo.api.domain.notification.event.MarathonCanceledEvent
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.domain.notification.support.NotificationEmailFactory
import com.rungo.api.global.infrastructure.mail.EmailMessage
import com.rungo.api.global.infrastructure.mail.entity.EmailOutbox
import com.rungo.api.global.infrastructure.mail.entity.EmailOutboxStatus
import com.rungo.api.global.infrastructure.mail.repository.EmailOutboxRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class NotificationEventListenerTest {

    @InjectMocks
    private lateinit var notificationEventListener: NotificationEventListener

    @Mock
    private lateinit var notificationEmailFactory: NotificationEmailFactory

    @Mock
    private lateinit var emailOutboxRepository: EmailOutboxRepository

    @Test
    @DisplayName("접수 완료 이벤트가 발행되면 이메일 발송 정보를 Outbox에 저장한다")
    fun handleRegistrationCompleted_save_outbox() {
        val event = RegistrationCompletedEvent(
            email = "user@test.com",
            marathonTitle = "서울 마라톤",
            courseName = "10K"
        )

        given(
            notificationEmailFactory.registrationCompleted(
                event.email,
                event.marathonTitle,
                event.courseName
            )
        ).willReturn(
            EmailMessage(
                to = "user@test.com",
                subject = "[Rungo] 참가 접수 완료",
                body = "서울 마라톤 10K 접수가 완료되었습니다."
            )
        )

        notificationEventListener.handleRegistrationCompleted(event)

        val captor = ArgumentCaptor.forClass(EmailOutbox::class.java)

        verify(emailOutboxRepository).save(captureEmailOutbox(captor))

        val savedOutbox = captor.value
        assertEquals("user@test.com", savedOutbox.recipient)
        assertEquals("[Rungo] 참가 접수 완료", savedOutbox.subject)
        assertEquals("서울 마라톤 10K 접수가 완료되었습니다.", savedOutbox.body)
        assertEquals(EmailOutboxStatus.PENDING, savedOutbox.status)
        assertEquals(0, savedOutbox.retryCount)
    }

    @Test
    @DisplayName("대회 취소 이벤트가 발행되면 참가자 수만큼 Outbox에 저장한다")
    fun handleMarathonCanceled_save_outboxes() {
        val event = MarathonCanceledEvent(
            marathonTitle = "서울 마라톤",
            participantEmails = listOf("user1@test.com", "user2@test.com")
        )

        given(
            notificationEmailFactory.marathonCanceled(
                "user1@test.com",
                event.marathonTitle
            )
        ).willReturn(
            EmailMessage(
                to = "user1@test.com",
                subject = "[Rungo] 대회 취소 안내",
                body = "서울 마라톤 대회가 취소되었습니다."
            )
        )

        given(
            notificationEmailFactory.marathonCanceled(
                "user2@test.com",
                event.marathonTitle
            )
        ).willReturn(
            EmailMessage(
                to = "user2@test.com",
                subject = "[Rungo] 대회 취소 안내",
                body = "서울 마라톤 대회가 취소되었습니다."
            )
        )

        notificationEventListener.handleMarathonCanceled(event)

        val captor = ArgumentCaptor.forClass(EmailOutbox::class.java)

        verify(emailOutboxRepository, times(2)).save(captureEmailOutbox(captor))

        val savedOutboxes = captor.allValues

        assertEquals(2, savedOutboxes.size)

        assertEquals("user1@test.com", savedOutboxes[0].recipient)
        assertEquals("[Rungo] 대회 취소 안내", savedOutboxes[0].subject)
        assertEquals("서울 마라톤 대회가 취소되었습니다.", savedOutboxes[0].body)
        assertEquals(EmailOutboxStatus.PENDING, savedOutboxes[0].status)
        assertEquals(0, savedOutboxes[0].retryCount)

        assertEquals("user2@test.com", savedOutboxes[1].recipient)
        assertEquals("[Rungo] 대회 취소 안내", savedOutboxes[1].subject)
        assertEquals("서울 마라톤 대회가 취소되었습니다.", savedOutboxes[1].body)
        assertEquals(EmailOutboxStatus.PENDING, savedOutboxes[1].status)
        assertEquals(0, savedOutboxes[1].retryCount)
    }

    private fun captureEmailOutbox(captor: ArgumentCaptor<EmailOutbox>): EmailOutbox {
        captor.capture()
        return EmailOutbox.create("", "", "")
    }
}