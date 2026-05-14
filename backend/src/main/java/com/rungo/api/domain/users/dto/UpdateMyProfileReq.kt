package com.rungo.api.domain.users.dto

import com.rungo.api.domain.users.enumtype.Gender
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "내 정보 수정 요청 DTO")
@JvmRecord
data class UpdateMyProfileReq(
    @field:Schema(description = "사용자 이름", example = "홍길동")
    @field:Size(min = 1, message = "이름은 1자 이상이어야 합니다.")
    @JvmField
    val name: String?,

    @field:Schema(description = "전화번호", example = "010-1234-5678")
    @field:Pattern(
        regexp = "^010-\\d{4}-\\d{4}$",
        message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)"
    )
    @JvmField
    val phoneNumber: String?,

    @field:Schema(description = "성별", example = "MALE")
    @JvmField
    val gender: Gender?,

    @field:Schema(description = "생년월일", example = "1990-01-01")
    @field:Past(
        message = "생년월일은 과거 날짜여야 합니다."
    )
    @JvmField
    val birth: LocalDate?,
) 