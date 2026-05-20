package com.rungo.api.domain.payment.service

import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.domain.payment.client.TossPaymentsClient
import com.rungo.api.domain.payment.client.TossPaymentsException
import com.rungo.api.domain.payment.dto.ConfirmPaymentReq
import com.rungo.api.domain.payment.dto.ConfirmPaymentRes
import com.rungo.api.domain.payment.entity.Payment
import com.rungo.api.domain.payment.enumtype.PaymentCancelResult
import com.rungo.api.domain.payment.enumtype.PaymentStatus
import com.rungo.api.domain.payment.repository.PaymentRepository
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import com.rungo.api.domain.registration.enumtype.RegistrationCancelReason
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val registrationRepository: RegistrationRepository,
    private val registrationCancelHistoryRepository: RegistrationCancelHistoryRepository,
    private val courseRepository: CourseRepository,
    private val tossPaymentsClient: TossPaymentsClient,
    private val eventPublisher: ApplicationEventPublisher,
    private val transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(noRollbackFor = [CustomException::class])
    fun confirm(userId: Long, request: ConfirmPaymentReq): ConfirmPaymentRes {
        val now = LocalDateTime.now()

        // 동일 orderId 동시 승인 방지를 위해 결제 건 조회
        val payment = paymentRepository.findByOrderIdForUpdate(request.orderId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)

        // 결제 소유자 및 금액 검증
        validatePaymentOwnerAndAmount(payment, userId, request)

        // 결제 상태별 confirm 가능 여부 확인
        when (payment.status) {
            PaymentStatus.DONE -> return confirmAlreadyCompletedPayment(payment, request)
            PaymentStatus.EXPIRED -> throw CustomException(ErrorCode.PAYMENT_EXPIRED)
            PaymentStatus.READY -> Unit
            else -> throw CustomException(ErrorCode.INVALID_PAYMENT_STATUS)
        }

        // 결제 유효시간 만료 시 결제 만료 및 접수 취소 처리
        if (!now.isBefore(payment.expiresAt)) {
            expireReadyPaymentAndRegistration(payment)
            throw CustomException(ErrorCode.PAYMENT_EXPIRED)
        }

        val registration = registrationRepository.findByIdOrNull(payment.originalRegistrationId)
            ?: throw CustomException(ErrorCode.REGISTRATION_NOT_FOUND)

        // 결제 대기 상태의 접수만 결제 완료 가능
        if (registration.status != RegistrationStatus.PENDING_PAYMENT) {
            throw CustomException(ErrorCode.INVALID_REGISTRATION_STATUS)
        }

        // 토스페이먼츠 confirm API 호출
        val tossPayment = try {
            tossPaymentsClient.confirm(
                paymentKey = request.paymentKey,
                orderId = request.orderId,
                amount = request.amount,
            )
        } catch (e: TossPaymentsException) {
            payment.recordConfirmFailure(e.code, e.message)
            throw CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED)
        }

        // 토스 승인 상태가 DONE인지 확인
        if (tossPayment.status != "DONE") {
            payment.recordConfirmFailure(
                tossPayment.status,
                "토스페이먼츠 결제 상태가 DONE이 아닙니다.",
            )
            throw CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED)
        }

        // 토스 응답의 주문 ID와 금액 검증
        validateTossConfirmResult(
            payment = payment,
            request = request,
            tossOrderId = tossPayment.orderId,
            tossTotalAmount = tossPayment.totalAmount,
        )

        // 결제 완료 처리
        payment.complete(
            paymentKey = tossPayment.paymentKey,
            method = tossPayment.method,
            approvedAt = parseTossDateTime(tossPayment.approvedAt) ?: now,
        )

        // 접수 결제 완료 처리
        registration.completePayment()

        // 결제 완료 알림 이벤트 발행
        eventPublisher.publishEvent(
            RegistrationCompletedEvent(
                registration.user.email,
                registration.marathon.title,
                registration.course.courseType,
            )
        )

        return ConfirmPaymentRes.from(payment, registration)
    }

    @Transactional(noRollbackFor = [CustomException::class])
    fun expirePendingPayments() {
        // 만료된 READY 결제 목록 조회
        val expiredPaymentIds = paymentRepository.findIdsByStatusAndExpiresAtLessThanEqual(
            status = PaymentStatus.READY,
            now = LocalDateTime.now(),
            pageable = PageRequest.of(0, EXPIRE_BATCH_SIZE),
        )

        // 각 결제 건을 독립 트랜잭션으로 만료 처리
        expiredPaymentIds.forEach { paymentId ->
            expirePaymentInNewTransaction(paymentId)
        }
    }

    fun processRequestedRefunds() {
        // 환불 요청 상태의 결제 목록 조회
        val requestedRefundPaymentIds = paymentRepository.findIdsByStatusIn(
            statuses = listOf(PaymentStatus.REFUND_REQUESTED),
            pageable = PageRequest.of(0, REFUND_BATCH_SIZE),
        )

        // 환불 요청 건 처리
        requestedRefundPaymentIds.forEach { paymentId ->
            processRefundInNewTransaction(
                paymentId = paymentId,
                allowProcessingRetry = false,
            )
        }

        // 오래된 환불 처리 중 건 재시도 대상 조회
        val staleProcessingPaymentIds = paymentRepository.findIdsByStatusAndUpdatedAtLessThanEqual(
            status = PaymentStatus.REFUND_PROCESSING,
            cutoff = LocalDateTime.now().minusMinutes(REFUND_PROCESSING_RETRY_MINUTES),
            pageable = PageRequest.of(0, REFUND_BATCH_SIZE),
        )

        // 환불 처리 중 멈춘 건 재시도
        staleProcessingPaymentIds.forEach { paymentId ->
            processRefundInNewTransaction(
                paymentId = paymentId,
                allowProcessingRetry = true,
            )
        }
    }

    @Transactional(noRollbackFor = [CustomException::class])
    fun cancelPaymentForRegistration(
        registrationId: Long,
        cancelReason: String,
    ): PaymentCancelResult {
        // 접수에 연결된 결제 건 조회
        val payment = paymentRepository.findByOriginalRegistrationIdForUpdate(registrationId)
            ?: return PaymentCancelResult.NOT_FOUND

        return when (payment.status) {
            PaymentStatus.READY -> {
                // 만료된 결제는 만료 처리, 유효한 결제는 취소 처리
                if (!LocalDateTime.now().isBefore(payment.expiresAt)) {
                    expireReadyPaymentAndRegistration(payment)
                    PaymentCancelResult.EXPIRED
                } else {
                    payment.cancel()
                    PaymentCancelResult.CANCELED
                }
            }

            // 결제 완료 건은 환불 요청 처리
            PaymentStatus.DONE -> {
                requestRefund(payment, cancelReason)
                PaymentCancelResult.REFUND_REQUESTED
            }

            PaymentStatus.EXPIRED -> PaymentCancelResult.EXPIRED

            else -> PaymentCancelResult.IGNORED
        }
    }

    @Transactional(noRollbackFor = [CustomException::class])
    fun cancelPaymentsForMarathonCancellation(
        registrationIds: Collection<Long>,
        cancelReason: String,
    ) {
        if (registrationIds.isEmpty()) return

        // 마라톤 취소 대상 접수들의 결제 건 조회
        val payments = paymentRepository.findByOriginalRegistrationIdInForUpdate(registrationIds)

        // 결제 대기 건은 취소, 결제 완료 건은 환불 요청
        payments.forEach { payment ->
            when (payment.status) {
                PaymentStatus.READY -> payment.cancel()
                PaymentStatus.DONE -> requestRefund(payment, cancelReason)
                else -> Unit
            }
        }
    }

    @Transactional(noRollbackFor = [CustomException::class])
    fun refundCompletedPaymentsForRegistrationIds(
        registrationIds: Collection<Long>,
        cancelReason: String,
    ) {
        if (registrationIds.isEmpty()) return

        // 결제 완료 상태의 결제 건만 조회
        val payments = paymentRepository.findByOriginalRegistrationIdInAndStatus(
            originalRegistrationIds = registrationIds,
            status = PaymentStatus.DONE,
        )

        // 결제 완료 건 환불 요청 처리
        payments.forEach { payment ->
            requestRefund(payment, cancelReason)
        }
    }

    @Transactional(noRollbackFor = [CustomException::class])
    fun retryFailedRefund(
        paymentId: Long,
        cancelReason: String = DEFAULT_REFUND_REASON,
    ) {
        // 실패한 환불 건 조회
        val payment = paymentRepository.findByIdForUpdate(paymentId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)

        // 환불 실패 상태만 재시도 가능
        if (payment.status != PaymentStatus.REFUND_FAILED) {
            throw CustomException(ErrorCode.INVALID_PAYMENT_STATUS)
        }

        requestRefund(payment, cancelReason)
    }

    private fun expirePaymentInNewTransaction(paymentId: Long) {
        runCatching {
            paymentTransactionTemplate.executeWithoutResult {
                // 만료 대상 결제 건 조회
                val payment = paymentRepository.findByIdForUpdate(paymentId)
                    ?: return@executeWithoutResult

                // READY 상태가 아니면 만료 처리하지 않음
                if (payment.status != PaymentStatus.READY) return@executeWithoutResult
                // 아직 만료 시간이 지나지 않았으면 무시
                if (LocalDateTime.now().isBefore(payment.expiresAt)) return@executeWithoutResult

                expireReadyPaymentAndRegistration(payment)
            }
        }.onFailure { e ->
            log.warn("Failed to expire payment. paymentId={}", paymentId, e)
        }
    }

    // 환불 대상 결제 처리
    private fun processRefundInNewTransaction(
        paymentId: Long,
        allowProcessingRetry: Boolean,
    ) {
        val refundTask = runCatching {
            paymentTransactionTemplate.execute<RefundTask?> {
                // 환불 대상 결제 건 조회
                val payment = paymentRepository.findByIdForUpdate(paymentId)
                    ?: return@execute null

                // 환불 처리 대상 상태 확인
                when (payment.status) {
                    PaymentStatus.REFUND_REQUESTED -> {
                        payment.markRefundProcessing()
                    }

                    PaymentStatus.REFUND_PROCESSING -> {
                        if (!allowProcessingRetry) {
                            return@execute null
                        }
                    }

                    else -> return@execute null
                }

                val paymentKey = payment.paymentKey

                // 결제 키가 없으면 환불 처리 불가
                if (paymentKey.isNullOrBlank()) {
                    payment.markRefundFailed(
                        "PAYMENT_KEY_NOT_FOUND",
                        "결제 키가 없어 환불할 수 없습니다.",
                    )
                    return@execute null
                }

                // 외부 환불 API 호출에 필요한 정보 생성
                RefundTask(
                    paymentId = payment.id,
                    paymentKey = paymentKey,
                    cancelReason = payment.refundReason ?: DEFAULT_REFUND_REASON,
                )
            }
        }.onFailure { e ->
            log.warn("Failed to start refund. paymentId={}", paymentId, e)
        }.getOrNull() ?: return

        try {
            // 토스 결제 취소 API 호출
            tossPaymentsClient.cancel(
                paymentKey = refundTask.paymentKey,
                cancelReason = refundTask.cancelReason,
                idempotencyKey = "refund-${refundTask.paymentId}",
            )

            paymentTransactionTemplate.executeWithoutResult {
                val payment = paymentRepository.findByIdForUpdate(refundTask.paymentId)
                    ?: return@executeWithoutResult

                // 환불 성공 처리
                if (payment.status == PaymentStatus.REFUND_PROCESSING) {
                    payment.markRefunded(LocalDateTime.now())
                }
            }
        } catch (e: TossPaymentsException) {
            log.warn(
                "Toss payment refund failed. paymentId={}, code={}, message={}",
                refundTask.paymentId,
                e.code,
                e.message,
            )

            paymentTransactionTemplate.executeWithoutResult {
                val payment = paymentRepository.findByIdForUpdate(refundTask.paymentId)
                    ?: return@executeWithoutResult

                // 토스 환불 실패 처리
                if (payment.status == PaymentStatus.REFUND_PROCESSING) {
                    payment.markRefundFailed(e.code, e.message)
                }
            }
        } catch (e: Exception) {
            log.warn(
                "Unexpected refund processing error. paymentId={}",
                refundTask.paymentId,
                e,
            )

            paymentTransactionTemplate.executeWithoutResult {
                val payment = paymentRepository.findByIdForUpdate(refundTask.paymentId)
                    ?: return@executeWithoutResult

                // 예상치 못한 환불 실패 처리
                if (payment.status == PaymentStatus.REFUND_PROCESSING) {
                    payment.markRefundFailed(
                        "REFUND_PROCESSING_ERROR",
                        e.message ?: "환불 처리 중 알 수 없는 오류가 발생했습니다.",
                    )
                }
            }
        }
    }

    private fun requestRefund(payment: Payment, cancelReason: String) {
        val paymentKey = payment.paymentKey

        // 결제 키가 없으면 환불 요청 불가
        if (paymentKey.isNullOrBlank()) {
            payment.markRefundFailed(
                "PAYMENT_KEY_NOT_FOUND",
                "결제 키가 없어 환불할 수 없습니다.",
            )
            return
        }

        // 환불 요청 상태로 변경
        payment.markRefundRequested(cancelReason)
    }

    // 결제 소유자 및 금액 검증
    private fun validatePaymentOwnerAndAmount(
        payment: Payment,
        userId: Long,
        request: ConfirmPaymentReq,
    ) {
        // 결제 요청 사용자 검증
        if (payment.userId != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        // 결제 금액 검증
        if (payment.amount != request.amount) {
            throw CustomException(ErrorCode.INVALID_PAYMENT_AMOUNT)
        }
    }

    // 토스 confirm 응답 검증
    private fun validateTossConfirmResult(
        payment: Payment,
        request: ConfirmPaymentReq,
        tossOrderId: String,
        tossTotalAmount: Long,
    ) {
        // 주문 ID 불일치 방지
        if (tossOrderId != request.orderId) {
            payment.recordConfirmFailure(
                "ORDER_ID_MISMATCH",
                "토스페이먼츠 승인 응답의 주문 ID가 서버 주문 ID와 일치하지 않습니다.",
            )
            throw CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED)
        }

        // 결제 금액 불일치 방지
        if (tossTotalAmount != payment.amount || tossTotalAmount != request.amount) {
            payment.recordConfirmFailure(
                "AMOUNT_MISMATCH",
                "토스페이먼츠 승인 응답의 결제 금액이 서버 결제 금액과 일치하지 않습니다.",
            )
            throw CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED)
        }
    }

    // 이미 완료된 결제 결과 반환
    private fun confirmAlreadyCompletedPayment(
        payment: Payment,
        request: ConfirmPaymentReq,
    ): ConfirmPaymentRes {
        // 기존 결제 키와 요청 결제 키가 다르면 잘못된 재요청
        if (payment.paymentKey != request.paymentKey) {
            throw CustomException(ErrorCode.INVALID_PAYMENT_STATUS)
        }

        val registration = registrationRepository.findByIdOrNull(payment.originalRegistrationId)
            ?: throw CustomException(ErrorCode.REGISTRATION_NOT_FOUND)

        return ConfirmPaymentRes.from(payment, registration)
    }

    // 결제 만료 및 접수 취소 처리
    private fun expireReadyPaymentAndRegistration(payment: Payment) {
        // READY 상태가 아니면 만료 처리하지 않음
        if (payment.status != PaymentStatus.READY) return

        val registration = registrationRepository.findByIdOrNull(payment.originalRegistrationId)

        // 결제 만료 상태로 변경
        payment.expire()

        // 접수가 없으면 결제 만료만 처리
        if (registration == null) {
            log.warn(
                "Payment expired but registration was not found. paymentId={}, registrationId={}",
                payment.id,
                payment.originalRegistrationId,
            )
            return
        }

        // 결제 대기 접수가 아니면 추가 처리하지 않음
        if (registration.status != RegistrationStatus.PENDING_PAYMENT) {
            log.warn(
                "Payment expired but registration status was not PENDING_PAYMENT. paymentId={}, registrationId={}, registrationStatus={}",
                payment.id,
                registration.id,
                registration.status,
            )
            return
        }

        // 접수 취소 이력 저장
        registrationCancelHistoryRepository.save(
            RegistrationCancelHistory.create(
                registration = registration,
                cancelReason = RegistrationCancelReason.PAYMENT_TIMEOUT,
            )
        )

        // 코스 정원 복구 및 접수 삭제
        courseRepository.decreaseCurrentCountIfPositive(registration.course.id)
        registrationRepository.delete(registration)
    }

    private fun parseTossDateTime(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null

        // 토스 승인 시간을 LocalDateTime으로 변환
        return runCatching {
            OffsetDateTime.parse(value).toLocalDateTime()
        }.getOrNull()
    }

    // 독립 트랜잭션 처리를 위한 템플릿
    private val paymentTransactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    // 환불 처리에 필요한 작업 정보
    private data class RefundTask(
        val paymentId: Long,
        val paymentKey: String,
        val cancelReason: String,
    )

    companion object {
        private const val EXPIRE_BATCH_SIZE = 500
        private const val REFUND_BATCH_SIZE = 100
        private const val DEFAULT_REFUND_REASON = "접수 취소 환불"
        private const val REFUND_PROCESSING_RETRY_MINUTES = 10L
    }
}