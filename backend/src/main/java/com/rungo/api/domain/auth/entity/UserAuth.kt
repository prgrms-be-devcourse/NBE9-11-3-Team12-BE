package com.rungo.api.domain.auth.entity

import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Provider
import jakarta.persistence.*

@Entity
@Table(
    name = "user_auth",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_provider_provider_id",
            columnNames = ["provider", "provider_id"]
        )
    ]
)
class UserAuth protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: Users
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    lateinit var provider: Provider
        protected set

    @Column(name = "provider_id", nullable = false, length = 255)
    lateinit var providerId: String
        protected set

    @Column(length = 255)
    var password: String? = null
        protected set

    companion object {
        @JvmStatic
        fun createLocalAuth(user: Users, encodedPassword: String): UserAuth =
            UserAuth().apply {
                this.user = user
                this.provider = Provider.LOCAL
                this.providerId = user.email
                this.password = encodedPassword
            }

        @JvmStatic
        fun createSocialAuth(
            user: Users,
            provider: Provider,
            providerId: String
        ): UserAuth =
            UserAuth().apply {
                this.user = user
                this.provider = provider
                this.providerId = providerId
                this.password = null
            }
    }
}