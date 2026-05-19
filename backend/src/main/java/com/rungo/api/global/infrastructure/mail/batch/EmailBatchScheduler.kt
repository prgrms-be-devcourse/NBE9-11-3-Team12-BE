package com.rungo.api.global.infrastructure.mail.batch

import EmailOutboxStatus
import com.rungo.api.global.infrastructure.mail.repository.EmailOutboxRepository
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Component
class EmailBatchScheduler(
    private val emailOutboxRepository: EmailOutboxRepository,
    private val emailOutboxProcessor: EmailOutboxProcessor,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val executor =
        Executors.newFixedThreadPool(MAIL_BATCH_THREAD_COUNT)

    @Scheduled(fixedDelay = MAIL_BATCH_FIXED_DELAY)
    @Transactional
    fun sendPendingEmails() {

        val targetOutboxes =
            emailOutboxRepository
                .findTop50ByStatusOrderByCreatedAtAsc(
                    EmailOutboxStatus.PENDING
                )

        if (targetOutboxes.isEmpty()) {
            return
        }

        targetOutboxes.forEach {
            it.markAsProcessing()
        }

        log.info(
            "이메일 배치 발송 시작 - 대상 수: {}",
            targetOutboxes.size
        )

        val futures =
            targetOutboxes.map { outbox ->

                CompletableFuture.runAsync({

                    runCatching {
                        emailOutboxProcessor.process(outbox.id)
                    }.onFailure { exception ->

                        log.error(
                            "이메일 배치 처리 중 예외 발생 [outboxId: {}]",
                            outbox.id,
                            exception,
                        )
                    }

                }, executor)
            }

        CompletableFuture
            .allOf(*futures.toTypedArray())
            .join()

        log.info("이메일 배치 발송 종료")
    }

    @PreDestroy
    fun shutdown() {
        log.info("이메일 배치 스레드 풀 종료")
        executor.shutdown()
    }

    companion object {

        private const val MAIL_BATCH_THREAD_COUNT = 5
        private const val MAIL_BATCH_FIXED_DELAY = 60_000L
    }
}