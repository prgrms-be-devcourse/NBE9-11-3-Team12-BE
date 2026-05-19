package com.rungo.api.domain.users.organizerApplication.dto

import jakarta.validation.constraints.NotBlank

data class OrganizerApplicationCreateReq(
    @field:NotBlank(message = "사업자등록번호는 필수입니다.")
    val businessRegistrationNumber: String,
)