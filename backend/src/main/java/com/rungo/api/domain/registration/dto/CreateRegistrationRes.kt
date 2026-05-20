package com.rungo.api.domain.registration.dto

import com.rungo.api.domain.payment.entity.Payment
import com.rungo.api.domain.payment.enumtype.PaymentStatus
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "접수 생성 응답 DTO")
data class CreateRegistrationRes(
    @field:Schema(description = "접수 ID", example = "100")
    val registrationId: Long,

    @field:Schema(description = "마라톤 ID", example = "1")
    val marathonId: Long,

    @field:Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
    val marathonTitle: String,

    @field:Schema(description = "코스 ID", example = "10")
    val courseId: Long,

    @field:Schema(description = "코스 종류", example = "10KM")
    val courseType: String,

    @field:Schema(description = "접수 상태", example = "PENDING_PAYMENT")
    val status: RegistrationStatus,

    @field:Schema(description = "결제 상태", example = "READY")
    val paymentStatus: PaymentStatus?,

    @field:Schema(description = "토스 결제 주문 ID", example = "REG-100-20200202233000-ABC123DEF4")
    val orderId: String?,

    @field:Schema(description = "결제 금액", example = "50000")
    val amount: Long?,

    @field:Schema(description = "결제 만료 시각", example = "2020-02-02T02:02:02")
    val paymentDueAt: LocalDateTime?,

    @field:Schema(description = "접수 시각", example = "2020-02-02T02:02:02")
    val appliedAt: LocalDateTime,
) {
    companion object {
        fun from(registration: Registration) = CreateRegistrationRes(
            registrationId = registration.id,
            marathonId = registration.marathon.id,
            marathonTitle = registration.marathon.title,
            courseId = registration.course.id,
            courseType = registration.course.courseType,
            status = registration.status,
            paymentStatus = null,
            orderId = null,
            amount = null,
            paymentDueAt = null,
            appliedAt = registration.appliedAt,
        )

        @JvmStatic
        fun from(registration: Registration, payment: Payment) = CreateRegistrationRes(
            registrationId = registration.id,
            marathonId = registration.marathon.id,
            marathonTitle = registration.marathon.title,
            courseId = registration.course.id,
            courseType = registration.course.courseType,
            status = registration.status,
            paymentStatus = payment.status,
            orderId = payment.orderId,
            amount = payment.amount,
            paymentDueAt = payment.expiresAt,
            appliedAt = registration.appliedAt,
        )
    }
}
