package com.rungo.api.domain.users.organizerApplication.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "주최자 권한 신청 요청 DTO")
data class OrganizerApplicationCreateReq(
    @field:NotBlank(message = "사업자등록번호는 필수입니다.")
    @field:Schema(description = "사업자 등록번호", example = "123-45-67890")
    val businessRegistrationNumber: String,
)