package com.rungo.api.domain.auth.dto;

import com.rungo.api.domain.users.enumtype.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 DTO")
public record LoginRes(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,
        @Schema(description = "이메일", example = "test@example.com")
        String email,
        @Schema(description = "사용자 이름", example = "홍길동")
        String name,
        @Schema(description = "권한", example = "USER")
        Role role

) {}