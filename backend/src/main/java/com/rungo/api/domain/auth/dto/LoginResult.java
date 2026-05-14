package com.rungo.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 처리 결과 DTO")
public record LoginResult(

        @Schema(description = "Access Token")
        String accessToken,

        @Schema(description = "Refresh Token")
        String refreshToken,

        @Schema(description = "로그인 응답 정보")
        LoginRes loginRes
) {}