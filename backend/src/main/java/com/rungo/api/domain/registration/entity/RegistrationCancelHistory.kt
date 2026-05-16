package com.rungo.api.domain.registration.entity

import com.rungo.api.domain.registration.enumtype.RegistrationCancelReason
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "registration_cancel_histories",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_registration_cancel_history_original_registration_id",
            columnNames = ["original_registration_id"]
        )
    ]
)
@EntityListeners(AuditingEntityListener::class)
class RegistrationCancelHistory protected constructor() {

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

    @Column(name = "snap_name", nullable = false, length = 50)
    lateinit var snapName: String
        protected set

    @Column(name = "snap_phone_number", nullable = false, length = 20)
    lateinit var snapPhoneNumber: String
        protected set

    @Column(name = "snap_zip_code", nullable = false, length = 10)
    lateinit var snapZipCode: String
        protected set

    @Column(name = "snap_address", nullable = false, length = 255)
    lateinit var snapAddress: String
        protected set

    @Column(name = "snap_detail", length = 255)
    var snapDetail: String? = null
        protected set

    @Column(name = "t_size", nullable = false, length = 10)
    lateinit var tSize: String
        protected set

    @Column(name = "agreed_terms", nullable = false)
    var isAgreedTerms: Boolean = false
        protected set

    @Column(name = "applied_at", nullable = false)
    lateinit var appliedAt: LocalDateTime
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", nullable = false)
    lateinit var cancelReason: RegistrationCancelReason
        protected set

    @CreatedDate
    @Column(name = "canceled_at", nullable = false)
    lateinit var canceledAt: LocalDateTime
        protected set

    companion object {
        @JvmStatic
        @JvmOverloads
        fun create(
            registration: Registration,
            cancelReason: RegistrationCancelReason = RegistrationCancelReason.USER_CANCELED
        ): RegistrationCancelHistory = RegistrationCancelHistory().apply {
            this.originalRegistrationId = registration.id
            this.userId = registration.user.id
            this.marathonId = registration.marathon.id
            this.courseId = registration.course.id
            this.snapName = registration.snapName
            this.snapPhoneNumber = registration.snapPhoneNumber
            this.snapZipCode = registration.snapZipCode
            this.snapAddress = registration.snapAddress
            this.snapDetail = registration.snapDetail
            this.tSize = registration.tSize
            this.isAgreedTerms = registration.isAgreedTerms
            this.appliedAt = registration.appliedAt
            this.cancelReason = cancelReason
        }
    }
}