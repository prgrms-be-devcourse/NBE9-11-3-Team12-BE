package com.rungo.api.domain.payment.dto

// 토스 결제 승인 요청
data class TossConfirmReq(
    val paymentKey: String,
    val orderId: String,
    val amount: Long,
)

// 토스 결제 취소 요청
data class TossCancelReq(
    val cancelReason: String,
    val cancelAmount: Long? = null,
)

// 토스 결제 응답
data class TossPaymentRes(
    val paymentKey: String,
    val orderId: String,
    val status: String,
    val method: String?,
    val totalAmount: Long,
    val approvedAt: String?,
)

// 토스 에러 응답
data class TossErrorRes(
    val code: String?,
    val message: String?,
)