package com.rungo.api.domain.notification.listener

import com.rungo.api.domain.notification.event.MarathonCanceledEvent
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.domain.notification.support.NotificationEmailFactory
import com.rungo.api.global.infrastructure.mail.EmailMessage
import com.rungo.api.global.infrastructure.mail.EmailService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class NotificationEventListenerTest {

    @Mock
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var notificationEmailFactory: NotificationEmailFactory

    @InjectMocks
    private lateinit var notificationEventListener: NotificationEventListener

    @Test
    @DisplayName("접수 완료 이벤트 발생 시 이메일 메시지를 생성하고 발송한다")
    fun handleRegistrationCompleted_test() {
        val event = RegistrationCompletedEvent(
            email = "test@gmail.com",
            marathonTitle = "서울 마라톤",
            courseName = "10km"
        )

        val message = EmailMessage(
            to = "test@gmail.com",
            subject = "[Rungo] 서울 마라톤 참가 접수 완료 안내",
            body = "body"
        )

        given(
            notificationEmailFactory.registrationCompleted(
                event.email,
                event.marathonTitle,
                event.courseName
            )
        ).willReturn(message)

        notificationEventListener.handleRegistrationCompleted(event)

        verify(notificationEmailFactory, times(1))
            .registrationCompleted(event.email, event.marathonTitle, event.courseName)
        verify(emailService, times(1)).send(message)
    }

    @Test
    @DisplayName("대회 취소 이벤트 발생 시 참가자 수만큼 이메일 메시지를 생성하고 발송한다")
    fun handleMarathonCanceled_test() {
        val emails = listOf("user1@gmail.com", "user2@gmail.com")
        val event = MarathonCanceledEvent(
            marathonTitle = "서울 마라톤",
            participantEmails = emails
        )

        val message1 = EmailMessage("user1@gmail.com", "subject1", "body1")
        val message2 = EmailMessage("user2@gmail.com", "subject2", "body2")

        given(notificationEmailFactory.marathonCanceled("user1@gmail.com", "서울 마라톤"))
            .willReturn(message1)
        given(notificationEmailFactory.marathonCanceled("user2@gmail.com", "서울 마라톤"))
            .willReturn(message2)

        notificationEventListener.handleMarathonCanceled(event)

        verify(notificationEmailFactory, times(1))
            .marathonCanceled("user1@gmail.com", "서울 마라톤")
        verify(notificationEmailFactory, times(1))
            .marathonCanceled("user2@gmail.com", "서울 마라톤")

        verify(emailService, times(1)).send(message1)
        verify(emailService, times(1)).send(message2)
    }
}