package com.rungo.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급 응답 DTO")
public record TokenRes(

        @Schema(description = "Access Token")
        String accessToken,

        @Schema(description = "Refresh Token")
        String refreshToken

) {}