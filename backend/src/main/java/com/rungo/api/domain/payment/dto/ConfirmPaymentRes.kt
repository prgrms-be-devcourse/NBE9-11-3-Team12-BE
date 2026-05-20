package com.rungo.api.domain.payment.dto

import com.rungo.api.domain.payment.entity.Payment
import com.rungo.api.domain.payment.enumtype.PaymentStatus
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import java.time.LocalDateTime

@JvmRecord
data class ConfirmPaymentRes(
    val registrationId: Long,
    val registrationStatus: RegistrationStatus,
    val orderId: String,
    val amount: Long,
    val paymentStatus: PaymentStatus,
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
