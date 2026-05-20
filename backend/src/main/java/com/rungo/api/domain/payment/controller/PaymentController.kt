package com.rungo.api.domain.payment.controller

import com.rungo.api.domain.payment.dto.ConfirmPaymentReq
import com.rungo.api.domain.payment.dto.ConfirmPaymentRes
import com.rungo.api.domain.payment.service.PaymentService
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.security.SecurityUser
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentService: PaymentService,
) {
    // 토스 결제 승인 요청
    @PostMapping("/confirm")
    fun confirm(
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @RequestBody request: ConfirmPaymentReq,
    ): ResponseEntity<ApiResponse<ConfirmPaymentRes>> {
        val result = paymentService.confirm(user.id, request)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }
}
