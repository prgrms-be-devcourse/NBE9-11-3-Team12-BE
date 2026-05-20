package com.rungo.api.domain.registration.service

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.domain.payment.entity.Payment
import com.rungo.api.domain.payment.enumtype.PaymentCancelResult
import com.rungo.api.domain.payment.repository.PaymentRepository
import com.rungo.api.domain.payment.service.PaymentService
import com.rungo.api.domain.payment.support.OrderIdGenerator
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.dto.CreateRegistrationRes
import com.rungo.api.domain.registration.dto.MyRegistrationRes
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import com.rungo.api.domain.registration.enumtype.MyRegistrationStatusFilter
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class RegistrationService(
    private val registrationRepository: RegistrationRepository,
    private val registrationCancelHistoryRepository: RegistrationCancelHistoryRepository,
    private val courseRepository: CourseRepository,
    private val marathonRepository: MarathonRepository,
    private val userRepository: UserRepository,
    private val paymentRepository: PaymentRepository,
    private val paymentService: PaymentService,
    private val orderIdGenerator: OrderIdGenerator,
    @Value("\${payment.expire-minutes:30}")
    private val paymentExpireMinutes: Long,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun create(userId: Long, request: CreateRegistrationReq): CreateRegistrationRes {
        if (!request.agreedTerms) {
            throw CustomException(ErrorCode.REGISTRATION_TERMS_REQUIRED)
        }

        val user = userRepository.findByIdOrNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (!user.isProfileCompleted) {
            throw CustomException(ErrorCode.PROFILE_NOT_COMPLETED)
        }

        val course = courseRepository.findByIdOrNull(request.courseId)
            ?: throw CustomException(ErrorCode.COURSE_NOT_FOUND)

        // 접수 생성 중 마라톤 취소/수정과 충돌하지 않도록 비관적 라 조회
        val marathon = marathonRepository.findByIdForUpdate(course.marathon.id)
            ?: throw CustomException(ErrorCode.MARATHON_NOT_FOUND)

        val now = LocalDateTime.now()

        if (now.isBefore(marathon.registrationStartAt) || now.isAfter(marathon.registrationEndAt)) {
            throw CustomException(ErrorCode.REGISTRATION_PERIOD_INVALID)
        }

        if (marathon.isCanceled()) {
            throw CustomException(ErrorCode.MARATHON_ALREADY_CANCELED)
        }

        // 코스 가격을 토스 결제 요청에 사용할 정수 금액으로 변환
        val amount = toPaymentAmount(course.price)

        // 무료 코스는 즉시 접수 완료, 유료 코스는 결제 대기 상태로 생성
        val registration = if (amount == 0L) {
            Registration.createCompleted(
                user = user,
                course = course,
                marathon = marathon,
                snapZipCode = request.snapZipCode,
                snapAddress = request.snapAddress,
                snapDetail = request.snapDetail,
                tSize = request.tSize,
                agreedTerms = request.agreedTerms,
            )
        } else {
            Registration.createPendingPayment(
                user = user,
                course = course,
                marathon = marathon,
                snapZipCode = request.snapZipCode,
                snapAddress = request.snapAddress,
                snapDetail = request.snapDetail,
                tSize = request.tSize,
                agreedTerms = request.agreedTerms,
            )
        }

        val updatedRows = courseRepository.increaseCurrentCountIfNotFull(course.id)
        if (updatedRows == 0) {
            throw CustomException(ErrorCode.CAPACITY_FULL)
        }

        val savedRegistration = registrationRepository.save(registration)

        // 무료 코스는 결제 없이 접수 완료 이벤트 발행
        if (amount == 0L) {
            publishRegistrationCompletedEvent(savedRegistration)
            return CreateRegistrationRes.from(savedRegistration)
        }

        // 토스 결제에 사용할 서버 주문 ID 생성
        val orderId = orderIdGenerator.generate(savedRegistration.id, now)

        // 유료 코스는 결제 대기 Payment 생성
        val payment = Payment.createReady(
            registration = savedRegistration,
            orderId = orderId,
            amount = amount,
            now = now,
            expireMinutes = paymentExpireMinutes,
        )

        val savedPayment = paymentRepository.save(payment)

        return CreateRegistrationRes.from(savedRegistration, savedPayment)
    }

    fun cancel(userId: Long, registrationId: Long) {
        val registration = registrationRepository.findByIdOrNull(registrationId)
        // 존재하지 않는 접수 건은 취소할 수 없다.
            ?: throw CustomException(ErrorCode.REGISTRATION_NOT_FOUND)

        // 본인 신청 건만 취소할 수 있다.
        if (registration.user.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val marathon = registration.marathon

        val now = LocalDateTime.now()

        // 접수 마감 이후에는 취소할 수 없다.
        if (now.isAfter(marathon.registrationEndAt)) {
            throw CustomException(ErrorCode.REGISTRATION_CANCEL_PERIOD_INVALID)
        }

        //취소된 마라톤은 접수 취소할 수 없다.
        if (marathon.isCanceled()) {
            throw CustomException(ErrorCode.MARATHON_ALREADY_CANCELED)
        }

        // 접수 취소 시 결제 상태에 따라 결제 취소 또는 환불 요청 처리
        val paymentCancelResult = paymentService.cancelPaymentForRegistration(
            registrationId = registration.id,
            cancelReason = "사용자 접수 취소",
        )

        // 결제 처리 결과에 따라 접수 취소 가능 여부 판단
        when (paymentCancelResult) {
            PaymentCancelResult.EXPIRED -> return

            PaymentCancelResult.NOT_FOUND -> {
                if (registration.status == RegistrationStatus.PENDING_PAYMENT) {
                    throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)
                }
            }

            PaymentCancelResult.IGNORED -> {
                throw CustomException(ErrorCode.INVALID_PAYMENT_STATUS)
            }

            PaymentCancelResult.CANCELED,
            PaymentCancelResult.REFUND_REQUESTED -> Unit
        }

        //유니크 제약을 바로 확인하기 위해 saveAndFlush
        registrationCancelHistoryRepository.saveAndFlush(
            RegistrationCancelHistory.create(registration)
        )

        courseRepository.decreaseCurrentCountIfPositive(registration.course.id)
        // 동시성 제어 미적용 메서드
        // registration.getCourse().decreaseCurrentCount();
        registrationRepository.delete(registration)
    }

    // status 필터에 따른 내 접수 목록 조회
    // ACTIVE   : 정상 접수인 상태 (취소 되지 않은 모든 접수)
    // CANCELED : 취소된 접수 상태
    @Transactional(readOnly = true)
    fun getMyRegistrations(
        userId: Long, status: MyRegistrationStatusFilter, pageable: Pageable,
    ): MyRegistrationRes = when (status) {
        MyRegistrationStatusFilter.ACTIVE -> getActiveRegistrations(userId, pageable)
        MyRegistrationStatusFilter.CANCELED -> getCanceledRegistrations(userId, pageable)
    }

    private fun getActiveRegistrations(userId: Long, pageable: Pageable): MyRegistrationRes {
        val sortedPageable: Pageable = PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            Sort.by(
                Sort.Order.desc("appliedAt"),
                Sort.Order.desc("id")
            )
        )
        val page = registrationRepository.findByUser_Id(userId, sortedPageable)

        val registrationIds = page.content.map { it.id }

        // 내 접수 목록에 결제 상태를 함께 보여주기 위해 결제 정보를 일괄 조회
        val paymentMap = if (registrationIds.isEmpty()) {
            emptyMap()
        } else {
            paymentRepository.findByOriginalRegistrationIdIn(registrationIds)
                .associateBy { it.originalRegistrationId }
        }

        val now = LocalDateTime.now()

        return MyRegistrationRes.fromActive(page, paymentMap, now)
    }

    private fun getCanceledRegistrations(userId: Long, pageable: Pageable): MyRegistrationRes {
        val sortedPageable: Pageable = PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            Sort.by(
                Sort.Order.desc("canceledAt"),
                Sort.Order.desc("id")
            )
        )

        val page = registrationCancelHistoryRepository.findByUserId(userId, sortedPageable)

        val marathonIds = page.content
            .map { it.marathonId }
            .toSet()

        val courseIds = page.content
            .map { it.courseId }
            .toSet()

        val marathonMap: Map<Long, Marathon> = marathonRepository.findAllById(marathonIds)
            .associateBy { it.id }

        val courseMap: Map<Long, Course> = courseRepository.findAllById(courseIds)
            .associateBy { it.id }

        val originalRegistrationIds = page.content
            .map { it.originalRegistrationId }
            .toSet()

        // 취소된 접수 목록에도 결제/환불 상태를 함께 보여주기 위해 결제 정보 조회
        val paymentMap = if (originalRegistrationIds.isEmpty()) {
            emptyMap()
        } else {
            paymentRepository.findByOriginalRegistrationIdIn(originalRegistrationIds)
                .associateBy { it.originalRegistrationId }
        }

        return MyRegistrationRes.fromCanceled(page, marathonMap, courseMap, paymentMap)
    }

    // 결제 금액 비교를 위해 코스 가격을 정수 금액으로 변환
    private fun toPaymentAmount(price: BigDecimal): Long {
        return try {
            price.toBigIntegerExact().longValueExact()
        } catch (e: ArithmeticException) {
            throw CustomException(ErrorCode.INVALID_PAYMENT_AMOUNT)
        }
    }

    // 무료 접수 완료 알림 이벤트 발행
    private fun publishRegistrationCompletedEvent(registration: Registration) {
        eventPublisher.publishEvent(
            RegistrationCompletedEvent(
                registration.user.email,
                registration.marathon.title,
                registration.course.courseType,
            )
        )
    }
}
