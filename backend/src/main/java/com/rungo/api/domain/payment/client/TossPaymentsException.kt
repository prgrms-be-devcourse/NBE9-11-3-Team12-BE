package com.rungo.api.domain.payment.client

// 토스페이먼츠 API 요청 실패 예외
class TossPaymentsException(
    val code: String?,
    override val message: String?,
) : RuntimeException(message)
