package com.rungo.api.global.infrastructure.mail.batch

import com.rungo.api.global.infrastructure.mail.entity.EmailOutbox
import com.rungo.api.global.infrastructure.mail.entity.EmailOutboxStatus
import com.rungo.api.global.infrastructure.mail.repository.EmailOutboxRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class EmailBatchSchedulerTest {

    @InjectMocks
    private lateinit var emailBatchScheduler: EmailBatchScheduler

    @Mock
    private lateinit var emailOutboxRepository: EmailOutboxRepository

    @Mock
    private lateinit var emailOutboxProcessor: EmailOutboxProcessor

    @AfterEach
    fun tearDown() {
        emailBatchScheduler.shutdown()
    }

    @Test
    @DisplayName("PENDING, FAILED 상태의 이메일이 없으면 Processor를 호출하지 않는다")
    fun sendPendingEmails_empty() {
        given(
            emailOutboxRepository.findTop50ByStatusInOrderByCreatedAtAsc(
                listOf(EmailOutboxStatus.PENDING, EmailOutboxStatus.FAILED)
            )
        ).willReturn(emptyList())

        emailBatchScheduler.sendPendingEmails()

        verify(emailOutboxProcessor, never()).process(1L)
    }

    @Test
    @DisplayName("PENDING, FAILED 상태의 이메일을 조회해 Processor에 위임한다")
    fun sendPendingEmails_success() {
        val outbox1 = createOutbox(1L)
        val outbox2 = createOutbox(2L)
        val outbox3 = createOutbox(3L)

        given(
            emailOutboxRepository.findTop50ByStatusInOrderByCreatedAtAsc(
                listOf(EmailOutboxStatus.PENDING, EmailOutboxStatus.FAILED)
            )
        ).willReturn(listOf(outbox1, outbox2, outbox3))

        emailBatchScheduler.sendPendingEmails()

        verify(emailOutboxProcessor).process(1L)
        verify(emailOutboxProcessor).process(2L)
        verify(emailOutboxProcessor).process(3L)
    }

    @Test
    @DisplayName("일부 이메일 처리 중 예외가 발생해도 스케줄러 전체는 중단되지 않는다")
    fun sendPendingEmails_processor_exception_isolated() {
        val outbox1 = createOutbox(1L)
        val outbox2 = createOutbox(2L)
        val outbox3 = createOutbox(3L)

        given(
            emailOutboxRepository.findTop50ByStatusInOrderByCreatedAtAsc(
                listOf(EmailOutboxStatus.PENDING, EmailOutboxStatus.FAILED)
            )
        ).willReturn(listOf(outbox1, outbox2, outbox3))

        doThrow(RuntimeException("processor error"))
            .`when`(emailOutboxProcessor)
            .process(2L)

        assertDoesNotThrow {
            emailBatchScheduler.sendPendingEmails()
        }

        verify(emailOutboxProcessor).process(1L)
        verify(emailOutboxProcessor).process(2L)
        verify(emailOutboxProcessor).process(3L)
    }

    private fun createOutbox(id: Long): EmailOutbox {
        val outbox = EmailOutbox.create(
            recipient = "user$id@test.com",
            subject = "메일 제목",
            body = "메일 본문"
        )

        ReflectionTestUtils.setField(outbox, "id", id)
        return outbox
    }
}