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
class Users(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var gender: Gender? = null,

    @Column
    var birth: LocalDate? = null,
) {
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

    val isProfileCompleted: Boolean
        get() = phoneNumber != null && gender != null && birth != null

    // java용 임시 빌더 추가 //
    companion object {
        @JvmStatic
        fun builder() = Builder()

        class Builder {
            private var id: Long? = null
            private var email: String = ""
            private var name: String = ""
            private var phoneNumber: String? = null
            private var role: Role = Role.PARTICIPANT
            private var gender: Gender? = null
            private var birth: LocalDate? = null

            fun id(id: Long) = apply { this.id = id }
            fun email(email: String) = apply { this.email = email }
            fun name(name: String) = apply { this.name = name }
            fun phoneNumber(phoneNumber: String?) = apply { this.phoneNumber = phoneNumber }
            fun role(role: Role) = apply { this.role = role }
            fun gender(gender: Gender?) = apply { this.gender = gender }
            fun birth(birth: LocalDate?) = apply { this.birth = birth }

            fun build() = Users(
                id = id,
                email = email,
                name = name,
                phoneNumber = phoneNumber,
                role = role,
                gender = gender,
                birth = birth,
            )
        }
    }
}