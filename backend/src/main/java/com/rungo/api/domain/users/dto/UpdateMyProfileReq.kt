package com.rungo.api.domain.users.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "내 정보 수정 요청 DTO")
@JvmRecord
data class UpdateMyProfileReq(

    @field:Schema(description = "사용자 이름", example = "홍길동")
    @field:Size(min = 1, message = "이름은 1자 이상이어야 합니다.")
    val name: String?,

    @field:Schema(description = "전화번호", example = "010-1234-5678")
    @field:Pattern(
        regexp = "^010-\\d{4}-\\d{4}$",
        message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)"
    )
    val phoneNumber: String?,
)