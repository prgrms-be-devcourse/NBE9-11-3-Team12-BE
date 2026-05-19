package com.rungo.api.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 재발급 응답 DTO")
data class TokenRes(
        @field:Schema(description = "Access Token")
        val accessToken: String,

        @field:Schema(description = "Refresh Token")
        val refreshToken: String
)