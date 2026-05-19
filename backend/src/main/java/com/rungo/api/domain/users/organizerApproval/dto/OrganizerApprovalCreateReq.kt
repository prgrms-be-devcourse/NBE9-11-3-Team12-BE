package com.rungo.api.domain.users.organizerApproval.dto

import jakarta.validation.constraints.NotBlank

data class OrganizerApprovalCreateReq(
    @field:NotBlank(message = "사업자등록번호는 필수입니다.")
    val businessRegistrationNumber: String,
)