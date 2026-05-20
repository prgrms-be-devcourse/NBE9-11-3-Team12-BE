package com.rungo.api.domain.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@JvmRecord
@Schema(description = "토스 결제 승인 요청 DTO")
data class ConfirmPaymentReq(
    @field:NotBlank
    @field:Schema(description = "토스페이먼츠 결제 키", example = "tgen_202002020001ABCDE")
    val paymentKey: String,

    @field:NotBlank
    @field:Schema(description = "백엔드에서 생성한 주문 ID", example = "REG-100-20200202233000-ABC123DEF4")
    val orderId: String,

    @field:Min(1)
    @field:Schema(description = "결제 승인 요청 금액", example = "50000")
    val amount: Long,
)
