package com.rungo.api.domain.users.organizerApplication.entity

import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "organizer_application")
@EntityListeners(AuditingEntityListener::class)
class OrganizerApplication protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: Users
        protected set

    @Column(nullable = false, length = 100)
    lateinit var businessRegistrationNumber: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var status: ApplicationStatus
        protected set

    @Column(length = 500)
    var rejectReason: String? = null
        protected set

    @CreatedDate
    @Column(nullable = false)
    lateinit var requestedAt: LocalDateTime
        protected set

    fun approve() {
        status = ApplicationStatus.APPROVED
        rejectReason = null
    }

    fun reject(reason: String){
        rejectReason = reason
        status = ApplicationStatus.REJECTED
    }
    companion object {
        fun create(
            user: Users,
            businessRegistrationNumber: String,
        ): OrganizerApplication =
            OrganizerApplication().apply {
                this.user = user
                this.businessRegistrationNumber = businessRegistrationNumber
                this.status = ApplicationStatus.PENDING
            }
    }
}
