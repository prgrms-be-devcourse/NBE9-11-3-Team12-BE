package com.rungo.api.global.infrastructure.mail.entity

import EmailOutboxStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_outbox")
class EmailOutbox protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @Column(nullable = false)
    lateinit var recipient: String
        protected set

    @Column(nullable = false)
    lateinit var subject: String
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    lateinit var body: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EmailOutboxStatus = EmailOutboxStatus.PENDING
        protected set

    @Column(nullable = false)
    var retryCount: Int = 0
        protected set

    @Column(length = 255)
    var lastErrorMessage: String? = null
        protected set

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    var sentAt: LocalDateTime? = null
        protected set

    fun markAsProcessing() {
        status = EmailOutboxStatus.PROCESSING
    }

    fun markAsSuccess() {
        status = EmailOutboxStatus.SUCCESS
        sentAt = LocalDateTime.now()
        lastErrorMessage = null
    }

    fun markAsFailed(errorMessage: String?) {
        retryCount += 1

        lastErrorMessage = errorMessage?.take(MAX_ERROR_MESSAGE_LENGTH)

        status =
            if (retryCount >= MAX_RETRY_COUNT) {
                EmailOutboxStatus.EXHAUSTED
            } else {
                EmailOutboxStatus.FAILED
            }
    }

    companion object {

        private const val MAX_RETRY_COUNT = 3
        private const val MAX_ERROR_MESSAGE_LENGTH = 255

        fun create(
            recipient: String,
            subject: String,
            body: String,
        ): EmailOutbox =
            EmailOutbox().apply {
                this.recipient = recipient
                this.subject = subject
                this.body = body
                this.status = EmailOutboxStatus.PENDING
                this.retryCount = 0
                this.createdAt = LocalDateTime.now()
            }
    }
}