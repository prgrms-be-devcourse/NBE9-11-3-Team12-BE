package com.rungo.api.domain.users.entity

import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Role
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class Users protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set

    @Column(nullable = false, unique = true, length = 100)
    lateinit var email: String
        protected set

    @Column(nullable = false, length = 50)
    lateinit var name: String
        protected set

    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var role: Role
        protected set

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: LocalDateTime
        protected set

    @Enumerated(EnumType.STRING)
    @Column
    var gender: Gender? = null
        protected set

    @Column
    var birth: LocalDate? = null
        protected set

    fun updateProfile(name: String?, phoneNumber: String?) {
        name?.let { this.name = it }
        phoneNumber?.let { this.phoneNumber = it }
    }

    fun completeProfile(name: String, phoneNumber: String, gender: Gender, birth: LocalDate) {
        this.name = name
        this.phoneNumber = phoneNumber
        this.gender = gender
        this.birth = birth
    }

    fun promoteToOrganizer() {
        this.role = Role.ORGANIZER
    }

    fun promoteToAdmin() {
        this.role = Role.ADMIN
    }

    val isProfileCompleted: Boolean
        get() = phoneNumber != null && gender != null && birth != null

    companion object {

        // 소셜 로그인 가입
        @JvmStatic
        fun createOAuth(
            email: String,
            name: String,
        ): Users = Users().apply {
            this.email = email
            this.name = name
            this.role = Role.PARTICIPANT
        }

        // 자체 회원가입
        @JvmStatic
        fun create(
            email: String,
            name: String,
            phoneNumber: String,
            gender: Gender,
            birth: LocalDate,
        ): Users = Users().apply {
            this.email = email
            this.name = name
            this.phoneNumber = phoneNumber
            this.role = Role.PARTICIPANT
            this.gender = gender
            this.birth = birth
        }
    }
}