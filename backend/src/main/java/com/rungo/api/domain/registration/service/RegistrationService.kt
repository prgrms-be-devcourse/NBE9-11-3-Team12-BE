package com.rungo.api.domain.registration.service

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.dto.CreateRegistrationRes
import com.rungo.api.domain.registration.dto.MyRegistrationRes
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import com.rungo.api.domain.registration.enumtype.MyRegistrationStatusFilter
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class RegistrationService(
    private val registrationRepository: RegistrationRepository,
    private val registrationCancelHistoryRepository: RegistrationCancelHistoryRepository,
    private val courseRepository: CourseRepository,
    private val marathonRepository: MarathonRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun create(userId: Long, request: CreateRegistrationReq): CreateRegistrationRes {
        // 필수 약관 미동의
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
        val marathon = course.marathon
        val now = LocalDateTime.now()

        // 접수 기간이 아니면 생성할 수 없다.
        if (now.isBefore(marathon.registrationStartAt) || now.isAfter(marathon.registrationEndAt)) {
            throw CustomException(ErrorCode.REGISTRATION_PERIOD_INVALID)
        }
        // 모집 중인 대회만 접수 가능하다.
//        if (!marathon.isOpen()) {
//            throw new CustomException(ErrorCode.MARATHON_NOT_OPEN);
//        }
        //취소된 마라톤은 접수할 수 없다.
        if (marathon.isCanceled()) {
            throw CustomException(ErrorCode.MARATHON_ALREADY_CANCELED)
        }

        val registration = Registration.create(
            user = user,
            course = course,
            marathon = marathon,
            snapZipCode = request.snapZipCode,
            snapAddress = request.snapAddress,
            snapDetail = request.snapDetail,
            tSize = request.tSize,
            agreedTerms = request.agreedTerms
        )

        val updatedRows = courseRepository.increaseCurrentCountIfNotFull(course.id)
        // 코스 정원이 가득 찼으면 접수를 막는다.
        if (updatedRows == 0) {
            throw CustomException(ErrorCode.CAPACITY_FULL)
        }
        // 동시성 제어 미적용 메서드
        // course.increaseCurrentCount();
        val savedRegistration = registrationRepository.save(registration)

        eventPublisher.publishEvent(
            RegistrationCompletedEvent(
                user.email,
                marathon.title,
                course.courseType
            )
        )

        return CreateRegistrationRes.from(savedRegistration)
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
        // 모집 중인 대회만 취소할 수 있다.
//        if (!marathon.isOpen()) {
//            throw new CustomException(ErrorCode.MARATHON_NOT_OPEN);
//        }
        //취소된 마라톤은 접수 취소할 수 없다.
        if (marathon.isCanceled()) {
            throw CustomException(ErrorCode.MARATHON_ALREADY_CANCELED)
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
    fun getMyRegistrations(userId: Long, status: MyRegistrationStatusFilter, pageable: Pageable,
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

        return MyRegistrationRes.fromActive(page)
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

        return MyRegistrationRes.fromCanceled(page, marathonMap, courseMap)
    }
}