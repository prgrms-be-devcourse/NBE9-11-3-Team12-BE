package com.rungo.api.domain.payment.enumtype

enum class PaymentStatus {
    READY,              // 결제 대기
    DONE,               // 결제 완료
    FAILED,             // 결제 실패
    EXPIRED,            // 결제 만료
    CANCELED,           // 결제 취소
    REFUND_REQUESTED,   // 환불 요청
    REFUND_PROCESSING,  // 환불 처리 중
    REFUNDED,           // 환불 완료
    REFUND_FAILED       // 환불 실패
}
