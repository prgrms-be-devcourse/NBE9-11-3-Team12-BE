package com.rungo.api.domain.registration.enumtype

enum class RegistrationStatus {
    PENDING_PAYMENT,    // 결제 대기
    COMPLETED,          // 접수 완료
    CANCELED            // 접수 취소
}
