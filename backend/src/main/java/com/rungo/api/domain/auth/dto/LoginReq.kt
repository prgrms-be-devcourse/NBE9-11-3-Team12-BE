package com.rungo.api.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "로그인 요청 DTO")
data class LoginReq(
        @field:Schema(description = "이메일", example = "test@example.com")
        @field:Email(message = "올바른 이메일 형식이 아닙니다.")
        @field:NotBlank(message = "이메일은 필수입니다.")
        val email: String,

        @field:Schema(description = "비밀번호", example = "Abcd1234!")
        @field:NotBlank(message = "비밀번호는 필수입니다.")
        val password: String
)