package com.rungo.api.domain.users.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "내 정보 수정 응답 DTO")
@JvmRecord
data class UpdateMyProfileRes(
    @field:Schema(description = "사용자 이름", example = "홍길동")
    val name: String,

    @field:Schema(description = "전화번호", example = "010-1234-5678")
    val phoneNumber: String?, // 소셜 로그인으로 가입한 경우 전화번호가 없을 수 있음.
)
