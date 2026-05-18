package com.rungo.api.global.infrastructure.mail

import com.rungo.api.global.infrastructure.mail.exception.EmailSendException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class EmailSenderClient(
    private val mailSender: JavaMailSender,

    @Value("\${spring.mail.username}")
    private val fromEmail: String,

    @Value("\${app.mail.enabled:true}")
    private val mailEnabled: Boolean
){
    @Retryable(
        retryFor = [EmailSendException::class],
        maxAttemptsExpression = "\${spring.mail.retry.max-attempts:3}",
        backoff = Backoff(
            delayExpression = "\${spring.mail.retry.delay:1000}",
            multiplierExpression = "\${spring.mail.retry.multiplier:2.0}"
        )
    )
    fun send(emailMessage: EmailMessage) {
        if (!mailEnabled) {
            log.info(
                "메일 전송 비활성화 상태 - skip: to={}, subject={}",
                emailMessage.to,
                emailMessage.subject
            )
            return
        }

        try {
            val message = SimpleMailMessage().apply {
                setTo(emailMessage.to)
                subject = emailMessage.subject
                text = emailMessage.body
                from = fromEmail
            }

            mailSender.send(message)

            log.info(
                "이메일 발송 성공: to={}, subject={}",
                emailMessage.to,
                emailMessage.subject
            )
        } catch (ex: MailException) {
            log.warn(
                "이메일 발송 실패 - 재시도 예정: to={}, subject={}",
                emailMessage.to,
                emailMessage.subject,
                ex
            )
            throw EmailSendException("이메일 발송 실패", ex)
        }
    }

    @Recover
    fun recover(ex: EmailSendException, emailMessage: EmailMessage) {
        log.error(
            "이메일 최종 발송 실패: to={}, subject={}",
            emailMessage.to,
            emailMessage.subject,
            ex
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(EmailSenderClient::class.java)
    }
}