package com.rungo.api.domain.users.admin.dto

import jakarta.validation.constraints.NotBlank

data class RejectOrganizerApplicationReq(
    @field:NotBlank(message = "거절 사유는 필수입니다.")
    val rejectReason: String,
)