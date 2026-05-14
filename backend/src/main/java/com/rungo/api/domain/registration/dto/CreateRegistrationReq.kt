package com.rungo.api.domain.registration.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "접수 생성 요청 DTO")
@JvmRecord
data class CreateRegistrationReq(
        @field:Schema(description = "신청할 코스 ID", example = "1")
        @field:NotNull
        @JvmField
        val courseId: Long?,

        @field:Schema(description = "접수 우편번호", example = "12345")
        @field:NotBlank
        @JvmField
        val snapZipCode: String?,

        @field:Schema(description = "접수 주소", example = "서울특별시 강남구 테헤란로 123")
        @field:NotBlank
        @JvmField
        val snapAddress: String?,

        @field:Schema(description = "접수 상세 주소", example = "101동 202호")
        @JvmField
        val snapDetail: String?,

        @field:Schema(description = "티셔츠 사이즈", example = "L")
        @field:NotBlank
        @JvmField
        val tSize: String?,

        @field:Schema(description = "약관 동의 여부", example = "true")
        @field:AssertTrue
        @JvmField
        val agreedTerms: Boolean,
)
