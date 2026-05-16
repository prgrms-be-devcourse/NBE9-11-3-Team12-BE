package com.rungo.api.domain.auth.dto

import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Role

@JvmRecord
data class MeRes(
    val id: Long,
    val email: String,
    val name: String,
    val role: Role,
    val profileCompleted: Boolean
) {
    companion object {
        @JvmStatic
        fun from(user: Users): MeRes = MeRes(
            id = user.id!!,
            email = user.email,
            name = user.name,
            role = user.role,
            profileCompleted = user.isProfileCompleted
        )
    }
}