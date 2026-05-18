package com.rungo.api.domain.marathon.marathon.entity

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import com.rungo.api.domain.marathon.marathon.enumtype.RecruitmentStatus
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(
    name = "Marathon",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_marathon_organizerId_title_eventDate",
            columnNames = ["organizer_id", "title", "event_date"]
        )
    ]
)
class Marathon protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    lateinit var organizer: Users
        protected set

    @Column(nullable = false, length = 200)
    lateinit var title: String
        protected set

    @Column(nullable = false, length = 50)
    lateinit var region: String
        protected set

    @Column(name = "detailed_address", nullable = false, length = 100)
    lateinit var detailedAddress: String
        protected set

    @Column(name = "event_date", nullable = false)
    lateinit var eventDate: LocalDate
        protected set

    @Column(length = 500)
    lateinit var posterImageUrl: String
        protected set

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @Column(name = "registration_start_at", nullable = false)
    lateinit var registrationStartAt: LocalDateTime
        protected set

    @Column(name = "registration_end_at", nullable = false)
    lateinit var registrationEndAt: LocalDateTime
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var status: MarathonStatus
        protected set

    @OneToMany(
        mappedBy = "marathon",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    private val _courses: MutableList<Course> = mutableListOf()

    val courses: List<Course>
        get() = _courses
    fun isOpen(): Boolean = status == MarathonStatus.OPEN

    fun isCanceled(): Boolean {
        return status == MarathonStatus.CANCELING ||
                status == MarathonStatus.CANCELED
    }

    fun addCourse(course: Course) {
        _courses.add(course)
        course.assignMarathon(this)
    }
    fun cancel() {
        if (isCanceled()) {
            throw CustomException(ErrorCode.MARATHON_ALREADY_CANCELED)
        }

        status = MarathonStatus.CANCELED
    }

    fun open() {
        status = MarathonStatus.OPEN
    }

    fun isAllCoursesFull(): Boolean {
        return courses.all { it.isFull() }
    }

    fun getRecruitmentStatus(): RecruitmentStatus {
        val now = LocalDateTime.now()

        if (isCanceled()) {
            return RecruitmentStatus.CANCELED
        }

        if (now.isBefore(registrationStartAt)) {
            return RecruitmentStatus.TEMP
        }

        if (now.isAfter(registrationEndAt)) {
            return RecruitmentStatus.CLOSED
        }

        if (isAllCoursesFull()) {
            return RecruitmentStatus.FULL
        }

        return RecruitmentStatus.OPEN
    }

    fun updateMarathonInfo(
        title: String?,
        region: String?,
        detailedAddress: String?,
        eventDate: LocalDate?,
        posterImageUrl: String?,
        registrationStartAt: LocalDateTime?,
        registrationEndAt: LocalDateTime?
    ) {
        title?.let { this.title = it }
        region?.let { this.region = it }
        detailedAddress?.let { this.detailedAddress = it }
        eventDate?.let { this.eventDate = it }
        posterImageUrl?.let { this.posterImageUrl = it }
        registrationStartAt?.let { this.registrationStartAt = it }
        registrationEndAt?.let { this.registrationEndAt = it }
    }

    companion object {

        @JvmStatic
        fun create(
            organizer: Users,
            title: String,
            region: String,
            detailedAddress: String,
            eventDate: LocalDate,
            posterImageUrl: String?,
            registrationStartAt: LocalDateTime,
            registrationEndAt: LocalDateTime
        ): Marathon {

            return Marathon().apply {
                this.organizer = organizer
                this.title = title
                this.region = region
                this.detailedAddress = detailedAddress
                this.eventDate = eventDate
                this.posterImageUrl = posterImageUrl
                this.registrationStartAt = registrationStartAt
                this.registrationEndAt = registrationEndAt
                this.status = MarathonStatus.OPEN
            }
        }
    }
}