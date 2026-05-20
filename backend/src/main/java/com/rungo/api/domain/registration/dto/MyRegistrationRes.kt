package com.rungo.api.domain.registration.dto

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.marathon.dto.PageRes
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.payment.entity.Payment
import com.rungo.api.domain.payment.enumtype.PaymentStatus
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "내 접수 목록 조회 응답 DTO")
data class MyRegistrationRes(

    @field:Schema(description = "접수 목록")
    val content: List<Item>,

    @field:Schema(description = "페이지 정보")
    val pageRes: PageRes,

    ) {

    companion object {

        // 정상 접수 목록 DTO 변환
        fun fromActive(
            page: Page<Registration>,
            paymentMap: Map<Long, Payment> = emptyMap(),
            now: LocalDateTime = LocalDateTime.now(),
        ) = MyRegistrationRes(
            content = page.content.map { registration ->
                Item.fromActive(
                    registration = registration,
                    payment = paymentMap[registration.id],
                    now = now,
                )
            },
            pageRes = PageRes.from(page)
        )

        // 접수 취소 목록 DTO 변환
        fun fromCanceled(
            page: Page<RegistrationCancelHistory>,
            marathonMap: Map<Long, Marathon>,
            courseMap: Map<Long, Course>,
            paymentMap: Map<Long, Payment> = emptyMap(),
        ) = MyRegistrationRes(
            content = page.content.map { history ->
                Item.fromCanceled(
                    history = history,
                    marathon = marathonMap.getValue(history.marathonId),
                    course = courseMap.getValue(history.courseId),
                    payment = paymentMap[history.originalRegistrationId],
                )
            },
            pageRes = PageRes.from(page)
        )
    }

    // ACTIVE, CANCELED 공통 구조 => 일부 필드는 null
    @Schema(description = "내 접수 목록 항목 DTO")
        data class Item(
        @field:Schema(description = "정상 접수 조회 시 현재 접수 ID / 취소 이력 조회 시 취소 이력 ID", example = "100")
        val registrationId: Long,

        @field:Schema(description = "취소 이력 조회 시 원본 접수 ID", example = "200")
        val historyId: Long?,

        @field:Schema(description = "마라톤 ID", example = "1")
        val marathonId: Long,

        @field:Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
        val marathonTitle: String,

        @field:Schema(description = "코스 ID", example = "10")
        val courseId: Long,

        @field:Schema(description = "코스 종류", example = "10KM")
        val courseType: String,

        @field:Schema(description = "접수 상태", example = "PENDING_PAYMENT")
        val status: String,

        @field:Schema(description = "결제 상태", example = "READY")
        val paymentStatus: PaymentStatus?,

        @field:Schema(description = "토스 결제 주문 ID", example = "REG-100-20200202233000-ABC123DEF4")
        val orderId: String?,

        @field:Schema(description = "결제 금액", example = "50000")
        val amount: Long?,

        @field:Schema(description = "결제 만료 시각", example = "2020-02-02T02:02:02")
        val paymentDueAt: LocalDateTime?,

        @field:Schema(description = "결제 승인 시각", example = "2020-02-02T02:02:02")
        val approvedAt: LocalDateTime?,

        @field:Schema(description = "결제 가능 여부", example = "true")
        val canPay: Boolean,

        @field:Schema(description = "환불 일시", example = "2020-02-02T02:02:02")
        val refundedAt: LocalDateTime?,

        @field:Schema(description = "환불 사유", example = "마라톤 취소로 인한 환불")
        val refundReason: String?,

        @field:Schema(description = "결제 실패 코드", example = "PAY_PROCESS_CANCELED")
        val failCode: String?,

        @field:Schema(description = "결제 실패 메시지", example = "사용자가 결제를 취소했습니다.")
        val failMessage: String?,

        @field:Schema(description = "결제 금액", example = "50000")
        val price: BigDecimal,

        @field:Schema(description = "대회 날짜", example = "2020-02-02")
        val eventDate: LocalDate,

        @field:Schema(description = "접수 이름", example = "홍길동")
        val snapName: String,

        @field:Schema(description = "접수 전화번호", example = "010-1234-5678")
        val snapPhoneNumber: String,

        @field:Schema(description = "접수 우편번호", example = "12345")
        val snapZipCode: String,

        @field:Schema(description = "접수 주소", example = "서울시 강남구 ...")
        val snapAddress: String,

        @field:Schema(description = "접수 상세주소", example = "101동 202호")
        val snapDetail: String?,

        @field:Schema(description = "티셔츠 사이즈", example = "L")
        val tSize: String,

        @field:Schema(description = "약관 동의 여부", example = "true")
        val agreedTerms: Boolean,

        @field:Schema(description = "접수 시각", example = "2020-02-02T02:02:02")
        val appliedAt: LocalDateTime,

        @field:Schema(description = "취소 시각", example = "2020-02-02T02:02:02")
        val canceledAt: LocalDateTime?,
    ) {

        // 정상 접수 Entity를 응답용 item 변환 (취소 전용 필드는 null : originalRegistrationId, canceledAt)
        companion object {

                fun fromActive(
                registration: Registration,
                payment: Payment? = null,
                now: LocalDateTime = LocalDateTime.now(),
            ) = Item(

                registrationId = registration.id,
                historyId = null,
                marathonId = registration.marathon.id,
                marathonTitle = registration.marathon.title,
                courseId = registration.course.id,
                courseType = registration.course.courseType,
                status = registration.status.name,
                paymentStatus = payment?.status,
                orderId = payment?.orderId,
                amount = payment?.amount,
                paymentDueAt = payment?.expiresAt,
                approvedAt = payment?.approvedAt,
                canPay = registration.status.name == "PENDING_PAYMENT" &&
                        payment?.status == PaymentStatus.READY &&
                        payment.expiresAt.isAfter(now),
                refundedAt = payment?.refundedAt,
                refundReason = payment?.refundReason,
                failCode = payment?.failCode,
                failMessage = payment?.failMessage,
                price = registration.course.price,
                eventDate = registration.marathon.eventDate,

                snapName = registration.snapName,
                snapPhoneNumber = registration.snapPhoneNumber,
                snapZipCode = registration.snapZipCode,
                snapAddress = registration.snapAddress,
                snapDetail = registration.snapDetail,
                tSize = registration.tSize,
                agreedTerms = registration.isAgreedTerms,

                appliedAt = registration.appliedAt,
                canceledAt = null,

                )

            // 취소 접수 Entity를 응답용 item 변환
                fun fromCanceled(
                history: RegistrationCancelHistory,
                marathon: Marathon,
                course: Course,
                payment: Payment? = null,
            ) = Item(
                registrationId = history.id,
                historyId = history.originalRegistrationId,
                marathonId = history.marathonId,
                marathonTitle = marathon.title,
                courseId = history.courseId,
                courseType = course.courseType,
                status = "CANCELED",
                paymentStatus = payment?.status,
                orderId = payment?.orderId,
                amount = payment?.amount,
                paymentDueAt = null,
                approvedAt = payment?.approvedAt,
                canPay = false,
                refundedAt = payment?.refundedAt,
                refundReason = payment?.refundReason,
                failCode = payment?.failCode,
                failMessage = payment?.failMessage,
                price = course.price,
                eventDate = marathon.eventDate,

                snapName = history.snapName,
                snapPhoneNumber = history.snapPhoneNumber,
                snapZipCode = history.snapZipCode,
                snapAddress = history.snapAddress,
                snapDetail = history.snapDetail,
                tSize = history.tSize,
                agreedTerms = history.isAgreedTerms,

                appliedAt = history.appliedAt,
                canceledAt = history.canceledAt,
            )
        }
    }
}
