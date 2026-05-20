package com.rungo.api.domain.payment.entity

import com.rungo.api.domain.payment.enumtype.PaymentStatus
import com.rungo.api.domain.registration.entity.Registration
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_payment_original_registration_id", columnNames = ["original_registration_id"]),
        UniqueConstraint(name = "uk_payment_order_id", columnNames = ["order_id"]),
        UniqueConstraint(name = "uk_payment_payment_key", columnNames = ["payment_key"]),
    ],
    indexes = [
        Index(name = "idx_payment_original_registration_id", columnList = "original_registration_id"),
        Index(name = "idx_payment_marathon_status", columnList = "marathon_id, status"),
        Index(name = "idx_payment_status_expires_at", columnList = "status, expires_at"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Payment protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set

    @Column(name = "original_registration_id", nullable = false)
    var originalRegistrationId: Long = 0L
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L
        protected set

    @Column(name = "marathon_id", nullable = false)
    var marathonId: Long = 0L
        protected set

    @Column(name = "course_id", nullable = false)
    var courseId: Long = 0L
        protected set

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    lateinit var orderId: String
        protected set

    @Column(name = "payment_key", unique = true, length = 200)
    var paymentKey: String? = null
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Long = 0L
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    lateinit var status: PaymentStatus
        protected set

    @Column(name = "method", length = 50)
    var method: String? = null
        protected set

    @Column(name = "requested_at", nullable = false)
    lateinit var requestedAt: LocalDateTime
        protected set

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null
        protected set

    @Column(name = "expires_at", nullable = false)
    lateinit var expiresAt: LocalDateTime
        protected set

    @Column(name = "refunded_at")
    var refundedAt: LocalDateTime? = null
        protected set

    @Column(name = "refund_reason", length = 200)
    var refundReason: String? = null
        protected set

    @Column(name = "fail_code", length = 100)
    var failCode: String? = null
        protected set

    @Column(name = "fail_message", length = 500)
    var failMessage: String? = null
        protected set

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: LocalDateTime
        protected set

    fun complete(paymentKey: String, method: String?, approvedAt: LocalDateTime) {
        this.paymentKey = paymentKey
        this.method = method
        this.approvedAt = approvedAt
        this.status = PaymentStatus.DONE
        this.failCode = null
        this.failMessage = null
    }

    fun fail(code: String?, message: String?) {
        this.status = PaymentStatus.FAILED
        this.failCode = code
        this.failMessage = message
    }

    fun recordConfirmFailure(code: String?, message: String?) {
        this.failCode = code
        this.failMessage = message
    }

    fun expire() {
        this.status = PaymentStatus.EXPIRED
    }

    fun cancel() {
        this.status = PaymentStatus.CANCELED
    }

    fun markRefundRequested(cancelReason: String) {
        this.status = PaymentStatus.REFUND_REQUESTED
        this.refundReason = cancelReason
        this.failCode = null
        this.failMessage = null
    }

    fun markRefundProcessing() {
        this.status = PaymentStatus.REFUND_PROCESSING
    }

    fun markRefunded(refundedAt: LocalDateTime) {
        this.status = PaymentStatus.REFUNDED
        this.refundedAt = refundedAt
        this.failCode = null
        this.failMessage = null
    }

    fun markRefundFailed(code: String?, message: String?) {
        this.status = PaymentStatus.REFUND_FAILED
        this.failCode = code
        this.failMessage = message
    }

    companion object {
        @JvmStatic
        fun createReady(
            registration: Registration,
            orderId: String,
            amount: Long,
            now: LocalDateTime,
            expireMinutes: Long,
        ): Payment = Payment().apply {
            this.originalRegistrationId = registration.id
            this.userId = registration.user.id
            this.marathonId = registration.marathon.id
            this.courseId = registration.course.id
            this.orderId = orderId
            this.amount = amount
            this.status = PaymentStatus.READY
            this.requestedAt = now
            this.expiresAt = now.plusMinutes(expireMinutes)
        }
    }
}
