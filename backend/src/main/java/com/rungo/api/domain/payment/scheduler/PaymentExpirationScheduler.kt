package com.rungo.api.domain.payment.scheduler

import com.rungo.api.domain.payment.service.PaymentService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PaymentExpirationScheduler(
    private val paymentService: PaymentService,
) {
    // 결제 유효시간이 지난 READY 결제 만료 처리
    @Scheduled(fixedDelayString = "\${payment.expire-scheduler-delay-ms:60000}")
    fun expirePendingPayments() {
        paymentService.expirePendingPayments()
    }

    // 환불 요청 및 재시도 대상 환불 처리
    @Scheduled(fixedDelayString = "\${payment.refund-scheduler-delay-ms:60000}")
    fun processRequestedRefunds() {
        paymentService.processRequestedRefunds()
    }
}