package com.rungo.api.domain.users.dto;

import com.rungo.api.domain.users.enumtype.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "내 정보 수정 요청 DTO")
public record UpdateMyProfileReq (

        @Schema(description = "이름", example = "홍길동")
        @Size(min = 1, message = "이름은 1자 이상이어야 합니다.")
        String name,

        @Schema(description = "전화번호", example = "010-1234-5678")
        @Pattern(
                regexp = "^010-\\d{4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)"
        )
        String phoneNumber,

        @Schema(description = "성별", example = "MALE")
        Gender gender,

        @Schema(description = "생년월일", example = "1990-01-01")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birth

) {}