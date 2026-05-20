package com.rungo.api.domain.payment.enumtype

enum class PaymentCancelResult {
    CANCELED,           // 결제 취소 완료
    REFUND_REQUESTED,   // 환불 요청 완료
    EXPIRED,            // 결제 만료 처리
    NOT_FOUND,          // 결제 건 없음
    IGNORED,            // 처리 대상 아님
}