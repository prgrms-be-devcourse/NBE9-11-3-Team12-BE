package com.rungo.api.global.infrastructure.mail.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "email_outbox")
class EmailOutbox protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @Column(name = "recipient", nullable = false)
    lateinit var recipient: String
        protected set

    @Column(name = "subject", nullable = false)
    lateinit var subject: String
        protected set

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    lateinit var body: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: EmailOutboxStatus = EmailOutboxStatus.PENDING
        protected set

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        protected set

    @Column(name = "last_error_message", length = 255)
    var lastErrorMessage: String? = null
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null
        protected set

    fun markAsSuccess() {
        this.status = EmailOutboxStatus.SUCCESS
        this.sentAt = LocalDateTime.now()
        this.lastErrorMessage = null
    }

    fun markAsFailed(errorMessage: String?) {
        this.retryCount += 1
        this.lastErrorMessage = errorMessage?.take(255)
        this.status = if (retryCount >= MAX_RETRY_COUNT) {
            EmailOutboxStatus.EXHAUSTED
        } else {
            EmailOutboxStatus.FAILED
        }
    }

    companion object {
        private const val MAX_RETRY_COUNT = 3

        fun create(
            recipient: String,
            subject: String,
            body: String
        ): EmailOutbox {
            val outbox = EmailOutbox()
            outbox.recipient = recipient
            outbox.subject = subject
            outbox.body = body
            outbox.status = EmailOutboxStatus.PENDING
            outbox.retryCount = 0
            outbox.createdAt = LocalDateTime.now()
            return outbox
        }
    }
}