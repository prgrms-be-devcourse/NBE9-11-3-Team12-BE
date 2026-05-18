package com.rungo.api.domain.notification.listener

import com.rungo.api.domain.notification.event.MarathonCanceledEvent
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.global.infrastructure.mail.EmailMessage
import com.rungo.api.global.infrastructure.mail.EmailService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
class NotificationIntegrationTest {

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @MockitoBean
    private lateinit var emailService: EmailService

    @Test
    @Transactional
    @DisplayName("트랜잭션 안에서 이벤트가 발행되면, 커밋 후 비동기로 이메일 발송이 호출된다")
    fun async_event_listener_integration_test() {
        val event = RegistrationCompletedEvent(
            email = "test@test.com",
            marathonTitle = "통합테스트 마라톤",
            courseName = "10km"
        )

        // 트랜잭션 내에서 이벤트 발행 (롤백 대기 상태)
        eventPublisher.publishEvent(event)

        // 테스트 트랜잭션 강제 커밋 후 종료 -> 리스너 작동
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // 커밋 완료, 비동기 스레드에서 메일 발송이 일어났는지 최대 2초 기다리며 검증
        verify(emailService, timeout(2000).times(1))
            .send(anyEmailMessage())
    }

    @Test
    @Transactional
    @DisplayName("트랜잭션이 롤백되면 이벤트가 발행되어도 이메일은 발송되지 않는다")
    fun rollback_event_listener_test() {
        val event = RegistrationCompletedEvent(
            email = "rollback@test.com",
            marathonTitle = "롤백 마라톤",
            courseName = "10km"
        )

        eventPublisher.publishEvent(event)

        TestTransaction.flagForRollback()
        TestTransaction.end()

        Thread.sleep(1000)

        // 한 번도 호출되지 않았음을 검증
        verify(emailService, never())
            .send(anyEmailMessage())
    }

    @Test
    @Transactional
    @DisplayName("대회 취소 이벤트가 발행되면, 커밋 후 비동기로 참가자 이메일 발송이 호출된다")
    fun marathon_cancel_event_listener_commit_test() {
        val event = MarathonCanceledEvent(
            marathonTitle = "서울 마라톤",
            participantEmails = listOf("user1@test.com", "user2@test.com")
        )

        eventPublisher.publishEvent(event)

        TestTransaction.flagForCommit()
        TestTransaction.end()

        verify(emailService, timeout(2000).times(2))
            .send(anyEmailMessage())
    }

    @Test
    @Transactional
    @DisplayName("대회 취소 이벤트가 발행되어도 트랜잭션이 롤백되면 이메일은 발송되지 않는다")
    fun marathon_cancel_event_listener_rollback_test() {
        val event = MarathonCanceledEvent(
            marathonTitle = "서울 마라톤",
            participantEmails = listOf("user1@test.com", "user2@test.com")
        )

        eventPublisher.publishEvent(event)

        TestTransaction.flagForRollback()
        TestTransaction.end()

        Thread.sleep(1000)

        verify(emailService, never())
            .send(anyEmailMessage())
    }

    private fun anyEmailMessage(): EmailMessage {
        any(EmailMessage::class.java)
        return EmailMessage("", "", "")
    }
}