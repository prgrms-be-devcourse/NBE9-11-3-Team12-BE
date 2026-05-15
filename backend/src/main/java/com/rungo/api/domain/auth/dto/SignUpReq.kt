package com.rungo.api.domain.auth.dto

import com.rungo.api.domain.users.enumtype.Gender
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.time.LocalDate

@JvmRecord
@Schema(description = "회원가입 요청 DTO")
data class SignUpReq(
    @field:Schema(description = "이메일", example = "test@example.com")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String,

    @field:Schema(description = "비밀번호", example = "Abcd1234!")
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#\$%^&*]).+\$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
    )
    val password: String,

    @field:Schema(description = "이름", example = "홍길동")
    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String,

    @field:Schema(description = "전화번호", example = "010-1234-5678")
    @field:NotBlank(message = "전화번호는 필수입니다.")
    @field:Pattern(
        regexp = "^010-\\d{4}-\\d{4}\$",
        message = "전화번호 형식은 010-xxxx-xxxx 이어야 합니다."
    )
    val phoneNumber: String,

    @field:Schema(description = "성별", example = "MALE")
    val gender: Gender,

    @field:Schema(description = "생년월일", example = "2000-01-01")
    @field:Past(message = "생년월일은 과거 날짜여야 합니다.")
    val birth: LocalDate
)