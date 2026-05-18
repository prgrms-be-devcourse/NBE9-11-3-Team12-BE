package com.rungo.api.domain.auth.dto

import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Role
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@JvmRecord
@Schema(description = "회원가입 응답 DTO")
data class SignUpRes(
        @field:Schema(description = "사용자 ID", example = "1")
        val id: Long,

        @field:Schema(description = "이메일", example = "test@example.com")
        val email: String,

        @field:Schema(description = "이름", example = "홍길동")
        val name: String,

        @field:Schema(description = "전화번호", example = "010-1234-5678")
        val phoneNumber: String,

        @field:Schema(description = "성별", example = "MALE")
        val gender: Gender,

        @field:Schema(description = "생년월일", example = "2000-01-01")
        val birth: LocalDate,

        @field:Schema(description = "권한", example = "PARTICIPANT")
        val role: Role,

        @field:Schema(description = "생성 시각", example = "2020-02-02T02:02:02")
        val createdAt: LocalDateTime
)