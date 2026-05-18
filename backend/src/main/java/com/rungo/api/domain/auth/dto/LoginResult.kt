package com.rungo.api.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 처리 결과 DTO")
data class LoginResult(
        @field:Schema(description = "Access Token")
        val accessToken: String,

        @field:Schema(description = "Refresh Token")
        val refreshToken: String,

        @field:Schema(description = "로그인 응답 정보")
        val loginRes: LoginRes
)