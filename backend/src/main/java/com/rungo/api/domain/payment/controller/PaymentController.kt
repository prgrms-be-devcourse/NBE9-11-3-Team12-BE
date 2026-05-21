package com.rungo.api.domain.payment.controller

import com.rungo.api.domain.payment.dto.ConfirmPaymentReq
import com.rungo.api.domain.payment.dto.ConfirmPaymentRes
import com.rungo.api.domain.payment.service.PaymentService
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment", description = "결제 관련 API")
@SecurityRequirement(name = "accessTokenCookie")
class PaymentController(
    private val paymentService: PaymentService,
) {
    // 토스 결제 승인 요청
    @PostMapping("/confirm")
    @Operation(
        summary = "토스 결제 승인",
        description = "토스페이먼츠 결제 승인 요청을 검증한 뒤, 성공한 경우 결제와 접수 상태를 완료로 변경합니다.",
    )
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "결제 승인 성공"),
        SwaggerResponse(responseCode = "400", description = "잘못된 결제 요청 또는 금액 불일치"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "404", description = "결제 또는 접수 정보 없음"),
        SwaggerResponse(responseCode = "409", description = "이미 처리되었거나 만료된 결제"),
    )
    fun confirm(
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @RequestBody request: ConfirmPaymentReq,
    ): ResponseEntity<ApiResponse<ConfirmPaymentRes>> {
        val result = paymentService.confirm(user.id, request)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }
}
