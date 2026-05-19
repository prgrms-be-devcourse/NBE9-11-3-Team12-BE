package com.rungo.api.domain.users.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RejectOrganizerApplicationReq(
    @field:Schema(description = "거절 사유", example = "사업자등록번호가 유효하지 않습니다.")
    @field:NotBlank(message = "거절 사유는 필수입니다.")
    val rejectReason: String,
)