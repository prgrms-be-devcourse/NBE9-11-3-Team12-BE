package com.rungo.api.global.infrastructure.mail.batch

import com.rungo.api.global.infrastructure.mail.EmailMessage
import com.rungo.api.global.infrastructure.mail.EmailService
import com.rungo.api.global.infrastructure.mail.entity.EmailOutbox
import com.rungo.api.global.infrastructure.mail.entity.EmailOutboxStatus
import com.rungo.api.global.infrastructure.mail.repository.EmailOutboxRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class EmailOutboxProcessorTest {

    @InjectMocks
    private lateinit var emailOutboxProcessor: EmailOutboxProcessor

    @Mock
    private lateinit var emailOutboxRepository: EmailOutboxRepository

    @Mock
    private lateinit var emailService: EmailService

    @Test
    @DisplayName("PENDING 상태의 이메일 발송에 성공하면 SUCCESS 상태로 변경된다")
    fun process_success() {
        val outbox = EmailOutbox.create(
            recipient = "user@test.com",
            subject = "접수 완료",
            body = "접수가 완료되었습니다."
        )

        given(emailOutboxRepository.findById(1L))
            .willReturn(Optional.of(outbox))

        emailOutboxProcessor.process(1L)

        verify(emailService).send(anyEmailMessage())

        assertEquals(EmailOutboxStatus.SUCCESS, outbox.status)
        assertEquals(0, outbox.retryCount)
        assertNull(outbox.lastErrorMessage)
        assertNotNull(outbox.sentAt)
    }

    @Test
    @DisplayName("이메일 발송에 실패하면 FAILED 상태로 변경되고 retryCount가 증가한다")
    fun process_fail() {
        val outbox = EmailOutbox.create(
            recipient = "user@test.com",
            subject = "접수 완료",
            body = "접수가 완료되었습니다."
        )

        given(emailOutboxRepository.findById(1L))
            .willReturn(Optional.of(outbox))

        doThrow(RuntimeException("SMTP Error"))
            .`when`(emailService)
            .send(anyEmailMessage())

        emailOutboxProcessor.process(1L)

        verify(emailService).send(anyEmailMessage())

        assertEquals(EmailOutboxStatus.FAILED, outbox.status)
        assertEquals(1, outbox.retryCount)
        assertEquals("SMTP Error", outbox.lastErrorMessage)
        assertNull(outbox.sentAt)
    }

    @Test
    @DisplayName("이메일 발송 실패가 3회 누적되면 EXHAUSTED 상태로 변경된다")
    fun process_fail_exhausted() {
        val outbox = EmailOutbox.create(
            recipient = "user@test.com",
            subject = "접수 완료",
            body = "접수가 완료되었습니다."
        )

        outbox.markAsFailed("1차 실패")
        outbox.markAsFailed("2차 실패")

        given(emailOutboxRepository.findById(1L))
            .willReturn(Optional.of(outbox))

        doThrow(RuntimeException("SMTP Error"))
            .`when`(emailService)
            .send(anyEmailMessage())

        emailOutboxProcessor.process(1L)

        verify(emailService).send(anyEmailMessage())

        assertEquals(EmailOutboxStatus.EXHAUSTED, outbox.status)
        assertEquals(3, outbox.retryCount)
        assertEquals("SMTP Error", outbox.lastErrorMessage)
        assertNull(outbox.sentAt)
    }

    @Test
    @DisplayName("대상 Outbox가 존재하지 않으면 이메일 발송을 수행하지 않는다")
    fun process_not_found() {
        given(emailOutboxRepository.findById(1L))
            .willReturn(Optional.empty())

        emailOutboxProcessor.process(1L)

        verify(emailService, never()).send(anyEmailMessage())
    }

    private fun anyEmailMessage(): EmailMessage {
        any(EmailMessage::class.java)
        return EmailMessage("", "", "")
    }
}