package com.rungo.api.domain.notification.listener

import com.rungo.api.domain.notification.event.MarathonCanceledEvent
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.global.infrastructure.mail.batch.EmailBatchScheduler
import com.rungo.api.global.infrastructure.mail.entity.EmailOutboxStatus
import com.rungo.api.global.infrastructure.mail.repository.EmailOutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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

    @Autowired
    private lateinit var emailOutboxRepository: EmailOutboxRepository

    @MockitoBean
    private lateinit var emailBatchScheduler: EmailBatchScheduler

    @AfterEach
    fun tearDown() {
        emailOutboxRepository.deleteAllInBatch()
    }

    @Test
    @Transactional
    @DisplayName("트랜잭션 안에서 접수 완료 이벤트가 발행되면, 커밋 시 Outbox에 이메일 발송 정보가 저장된다")
    fun registration_completed_event_commit_saves_outbox() {
        val event = RegistrationCompletedEvent(
            email = "test@test.com",
            marathonTitle = "통합테스트 마라톤",
            courseName = "10km"
        )

        eventPublisher.publishEvent(event)

        TestTransaction.flagForCommit()
        TestTransaction.end()

        val outboxes = emailOutboxRepository.findAll()

        assertThat(outboxes).hasSize(1)

        val outbox = outboxes[0]

        assertThat(outbox.recipient).isEqualTo("test@test.com")
        assertThat(outbox.subject).contains("참가 접수 완료")
        assertThat(outbox.body).contains("통합테스트 마라톤")
        assertThat(outbox.body).contains("10km")
        assertThat(outbox.status).isEqualTo(EmailOutboxStatus.PENDING)
        assertThat(outbox.retryCount).isEqualTo(0)
    }

    @Test
    @Transactional
    @DisplayName("트랜잭션이 롤백되면 접수 완료 이벤트가 발행되어도 Outbox에 저장되지 않는다")
    fun registration_completed_event_rollback_does_not_save_outbox() {
        val beforeCount = emailOutboxRepository.count()

        val event = RegistrationCompletedEvent(
            email = "rollback@test.com",
            marathonTitle = "롤백 마라톤",
            courseName = "10km"
        )

        eventPublisher.publishEvent(event)

        TestTransaction.flagForRollback()
        TestTransaction.end()

        assertThat(emailOutboxRepository.count()).isEqualTo(beforeCount)
    }

    @Test
    @Transactional
    @DisplayName("대회 취소 이벤트가 발행되면, 커밋 시 참가자 수만큼 Outbox에 이메일 발송 정보가 저장된다")
    fun marathon_cancel_event_commit_saves_outboxes() {
        val beforeCount = emailOutboxRepository.count()

        val event = MarathonCanceledEvent(
            marathonTitle = "서울 마라톤",
            participantEmails = listOf("user1@test.com", "user2@test.com")
        )

        eventPublisher.publishEvent(event)

        TestTransaction.flagForCommit()
        TestTransaction.end()

        assertThat(emailOutboxRepository.count()).isEqualTo(beforeCount + 2)

        val createdOutboxes = emailOutboxRepository.findAll()
            .filter {
                it.recipient in listOf(
                    "user1@test.com",
                    "user2@test.com"
                )
            }
            .sortedBy { it.recipient }

        assertThat(createdOutboxes).hasSize(2)

        assertThat(createdOutboxes[0].recipient).isEqualTo("user1@test.com")
        assertThat(createdOutboxes[0].subject).contains("대회 취소")
        assertThat(createdOutboxes[0].body).contains("서울 마라톤")
        assertThat(createdOutboxes[0].status).isEqualTo(EmailOutboxStatus.PENDING)

        assertThat(createdOutboxes[1].recipient).isEqualTo("user2@test.com")
        assertThat(createdOutboxes[1].subject).contains("대회 취소")
        assertThat(createdOutboxes[1].body).contains("서울 마라톤")
        assertThat(createdOutboxes[1].status).isEqualTo(EmailOutboxStatus.PENDING)
    }

    @Test
    @Transactional
    @DisplayName("대회 취소 이벤트가 발행되어도 트랜잭션이 롤백되면 Outbox에 저장되지 않는다")
    fun marathon_cancel_event_rollback_does_not_save_outbox() {
        val beforeCount = emailOutboxRepository.count()

        val event = MarathonCanceledEvent(
            marathonTitle = "서울 마라톤",
            participantEmails = listOf("user1@test.com", "user2@test.com")
        )

        eventPublisher.publishEvent(event)

        TestTransaction.flagForRollback()
        TestTransaction.end()

        assertThat(emailOutboxRepository.count()).isEqualTo(beforeCount)
    }
}