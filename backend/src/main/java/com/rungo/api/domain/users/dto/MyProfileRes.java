package com.rungo.api.domain.users.dto;

import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "내 정보 조회 응답 DTO")
public record MyProfileRes(

        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "이메일", example = "test@example.com")
        String email,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "성별", example = "MALE")
        Gender gender,

        @Schema(description = "생년월일", example = "2000-01-01")
        LocalDate birth,

        @Schema(description = "권한", example = "PARTICIPANT")
        Role role

) {}