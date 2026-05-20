package com.rungo.api.domain.auth.repository

import com.rungo.api.domain.auth.entity.UserAuth
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Provider
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface UserAuthRepository : JpaRepository<UserAuth, Long> {

    @EntityGraph(attributePaths = ["user"])
    fun findByProviderAndProviderId(
        provider: Provider,
        providerId: String
    ): UserAuth?

    fun findByUser_EmailAndProvider(
        email: String,
        provider: Provider
    ): UserAuth?

    fun existsByUserAndProvider(
        user: Users,
        provider: Provider
    ): Boolean
}