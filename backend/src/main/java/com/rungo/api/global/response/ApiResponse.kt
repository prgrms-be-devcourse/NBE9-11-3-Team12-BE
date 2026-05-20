package com.rungo.api.global.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus

@Schema(description = "공통 API 응답 래퍼")
data class ApiResponse<T>(
    @Schema(description = "HTTP 상태 코드", example = "200") val status: Int,
    @Schema(description = "응답 코드", example = "SUCCESS") val code: String,
    @Schema(description = "응답 메시지", example = "요청에 성공했습니다.") val message: String,
    @Schema(description = "실제 응답 데이터") val data: T? = null
) {
    companion object {
        // 1. 조회 성공 (200 OK + 데이터 반환)
        fun <T> ok(data: T): ApiResponse<T> =
            ApiResponse(HttpStatus.OK.value(), "SUCCESS", "요청에 성공했습니다.", data)

        // 2. 생성 성공 (201 Created + 커스텀 메시지 + 데이터 반환)
        fun <T> created(message: String, data: T): ApiResponse<T> =
            ApiResponse(HttpStatus.CREATED.value(), "SUCCESS", message, data)

        // 3. 데이터 없는 성공 (200 OK + 커스텀 메시지 반환)
        fun okMessage(message: String): ApiResponse<Void?> =
            ApiResponse(HttpStatus.OK.value(), "SUCCESS", message)

        // 4. 에러 응답 (기본 + 상세 데이터 선택 가능)
        fun <T> error(
            status: HttpStatus,
            code: String,
            message: String,
            data: T? = null
        ): ApiResponse<T> =
            ApiResponse(status.value(), code, message, data)
    }
}