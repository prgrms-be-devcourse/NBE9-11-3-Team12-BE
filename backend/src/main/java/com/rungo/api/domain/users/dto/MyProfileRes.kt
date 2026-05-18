package com.rungo.api.domain.users.dto

import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Role
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "내 정보 조회 응답 DTO")
@JvmRecord
data class MyProfileRes(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @field:Schema(description = "사용자 이메일", example = "user@test.com")
    val email: String,

    @field:Schema(description = "사용자 이름", example = "홍길동")
    val name: String,

    @field:Schema(description = "전화번호", example = "010-1234-5678")
    val phoneNumber: String?,

    @field:Schema(description = "성별", example="MALE")
    val gender: Gender?,

    @field:Schema(description = "생년월일", example = "1990-01-01")
    val birth: LocalDate?,

    @field:Schema(description = "사용자 역할", example = "USER")
    val role: Role
) 