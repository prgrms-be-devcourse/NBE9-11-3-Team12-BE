package com.rungo.api.domain.auth.dto

import com.rungo.api.domain.users.enumtype.Role
import io.swagger.v3.oas.annotations.media.Schema

@JvmRecord
@Schema(description = "로그인 응답 DTO")
data class LoginRes(
        @field:Schema(description = "사용자 ID", example = "1") val userId: Long,
        @field:Schema(description = "이메일", example = "test@example.com") val email: String,
        @field:Schema(description = "사용자 이름", example = "홍길동") val name: String,
        @field:Schema(description = "권한", example = "USER") val role: Role
)