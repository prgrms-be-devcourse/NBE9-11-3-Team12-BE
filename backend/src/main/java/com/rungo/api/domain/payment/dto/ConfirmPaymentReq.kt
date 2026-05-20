package com.rungo.api.domain.payment.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@JvmRecord
data class ConfirmPaymentReq(
    @field:NotBlank
    val paymentKey: String,

    @field:NotBlank
    val orderId: String,

    @field:Min(1)
    val amount: Long,
)