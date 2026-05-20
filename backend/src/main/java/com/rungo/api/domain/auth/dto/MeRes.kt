package com.rungo.api.domain.auth.dto

import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Role
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 사용자 정보 응답 DTO")
data class MeRes(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @field:Schema(description = "이메일", example = "test@example.com")
    val email: String,

    @field:Schema(description = "이름", example = "홍길동")
    val name: String,

    @field:Schema(description = "권한", example = "PARTICIPANT")
    val role: Role,

    @field:Schema(description = "프로필 정보 완성 여부", example = "true")
    val profileCompleted: Boolean
) {
    companion object {
        fun from(user: Users): MeRes = MeRes(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role,
            profileCompleted = user.isProfileCompleted
        )
    }
}