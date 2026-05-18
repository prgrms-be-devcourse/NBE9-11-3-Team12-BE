package com.rungo.api.domain.registration.entity

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "registrations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_registration_user_marathon",
            columnNames = ["user_id", "marathon_id"]
        )
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Registration protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: Users
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    lateinit var course: Course
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marathon_id", nullable = false)
    lateinit var marathon: Marathon
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    lateinit var status: RegistrationStatus
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

    @CreatedDate
    @Column(name = "applied_at", nullable = false)
    lateinit var appliedAt: LocalDateTime
        protected set

    @Column(name = "agreed_terms", nullable = false)
    var isAgreedTerms: Boolean = false
        protected set

    companion object {
        @JvmStatic
        fun create(
            user: Users,
            course: Course,
            marathon: Marathon,
            snapZipCode: String,
            snapAddress: String,
            snapDetail: String?,
            tSize: String,
            agreedTerms: Boolean
        ): Registration = Registration().apply {
            this.user = user
            this.course = course
            this.marathon = marathon
            this.status = RegistrationStatus.COMPLETED
            this.snapName = user.name
            this.snapPhoneNumber = user.phoneNumber
                ?: throw CustomException(ErrorCode.PROFILE_NOT_COMPLETED)
            this.snapZipCode = snapZipCode
            this.snapAddress = snapAddress
            this.snapDetail = snapDetail
            this.tSize = tSize
            this.isAgreedTerms = agreedTerms
        }
    }
}
