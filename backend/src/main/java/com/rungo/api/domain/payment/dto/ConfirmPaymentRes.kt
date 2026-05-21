package com.rungo.api.domain.payment.dto

import com.rungo.api.domain.payment.entity.Payment
import com.rungo.api.domain.payment.enumtype.PaymentStatus
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JvmRecord
@Schema(description = "토스 결제 승인 응답 DTO")
data class ConfirmPaymentRes(
    @field:Schema(description = "접수 ID", example = "100")
    val registrationId: Long,

    @field:Schema(description = "접수 상태", example = "COMPLETED")
    val registrationStatus: RegistrationStatus,

    @field:Schema(description = "토스 결제 주문 ID", example = "REG-100-20200202233000-ABC123DEF4")
    val orderId: String,

    @field:Schema(description = "결제 금액", example = "50000")
    val amount: Long,

    @field:Schema(description = "결제 상태", example = "DONE")
    val paymentStatus: PaymentStatus,

    @field:Schema(description = "결제 승인 시각", example = "2020-02-02T02:02:02")
    val approvedAt: LocalDateTime?,
) {
    companion object {
        @JvmStatic
        fun from(payment: Payment, registration: Registration): ConfirmPaymentRes = ConfirmPaymentRes(
            registrationId = registration.id,
            registrationStatus = registration.status,
            orderId = payment.orderId,
            amount = payment.amount,
            paymentStatus = payment.status,
            approvedAt = payment.approvedAt,
        )
    }
}
