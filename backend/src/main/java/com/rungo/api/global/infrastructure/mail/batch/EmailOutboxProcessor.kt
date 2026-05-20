package com.rungo.api.global.infrastructure.mail.batch

import com.rungo.api.global.infrastructure.mail.EmailMessage
import com.rungo.api.global.infrastructure.mail.EmailService
import com.rungo.api.global.infrastructure.mail.repository.EmailOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class EmailOutboxProcessor(
    private val emailOutboxRepository: EmailOutboxRepository,
    private val emailService: EmailService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun process(outboxId: Long) {

        val outbox =
            emailOutboxRepository.findByIdOrNull(outboxId)
                ?: return

        runCatching {

            emailService.send(
                EmailMessage(
                    to = outbox.recipient,
                    subject = outbox.subject,
                    body = outbox.body,
                )
            )

            outbox.markAsSuccess()

            log.info(
                "이메일 발송 성공 [outboxId: {}, recipient: {}]",
                outbox.id,
                outbox.recipient,
            )

        }.onFailure { exception ->

            outbox.markAsFailed(exception.message)

            log.warn(
                "이메일 발송 실패 [outboxId: {}, retryCount: {}, status: {}]",
                outbox.id,
                outbox.retryCount,
                outbox.status,
                exception,
            )
        }
    }
}