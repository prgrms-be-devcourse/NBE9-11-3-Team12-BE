package com.rungo.api.domain.registration.enumtype

enum class RegistrationCancelReason {
    USER_CANCELED,      // 사용자 직접 취소
    MARATHON_CANCELED,  // 마라톤 취소로 인한 접수 취소
    PAYMENT_TIMEOUT     // 결제 시간 만료로 인한 접수 취소
}
