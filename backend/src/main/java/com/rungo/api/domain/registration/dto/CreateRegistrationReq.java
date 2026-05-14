package com.rungo.api.domain.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "접수 생성 요청 DTO")
public record CreateRegistrationReq(

        @Schema(description = "신청할 코스 ID", example = "1")
        @NotNull
        Long courseId,

        @Schema(description = "접수 우편번호", example = "12345")
        @NotBlank
        String snapZipCode,

        @Schema(description = "접수 주소", example = "서울특별시 강남구 테헤란로 123")
        @NotBlank
        String snapAddress,

        @Schema(description = "접수 상세 주소", example = "101동 202호")
        String snapDetail,

        @Schema(description = "티셔츠 사이즈", example = "L")
        @NotBlank
        String tSize,

        @Schema(description = "약관 동의 여부", example = "true")
        @AssertTrue
        boolean agreedTerms
) {
}
