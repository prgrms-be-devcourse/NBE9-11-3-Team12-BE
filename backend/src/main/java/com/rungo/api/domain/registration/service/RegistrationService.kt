package com.rungo.api.domain.registration.service

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.dto.CreateRegistrationRes
import com.rungo.api.domain.registration.dto.CreateRegistrationRes.Companion.from
import com.rungo.api.domain.registration.dto.MyRegistrationRes
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import com.rungo.api.domain.registration.enumtype.MyRegistrationStatusFilter
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import lombok.RequiredArgsConstructor
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors

@Service
@RequiredArgsConstructor
@Transactional
class RegistrationService {
    private val registrationRepository: RegistrationRepository? = null
    private val registrationCancelHistoryRepository: RegistrationCancelHistoryRepository? = null
    private val courseRepository: CourseRepository? = null
    private val marathonRepository: MarathonRepository? = null
    private val userRepository: UserRepository? = null
    private val eventPublisher: ApplicationEventPublisher? = null

    fun create(userId: Long, request: CreateRegistrationReq): CreateRegistrationRes {
        // 필수 약관 미동의

        if (!request.agreedTerms) {
            throw CustomException(ErrorCode.REGISTRATION_TERMS_REQUIRED)
        }

        val user = userRepository!!.findById(userId)
            .orElseThrow<CustomException?>(Supplier { CustomException(ErrorCode.USER_NOT_FOUND) })

        if (!user.isProfileCompleted()) {
            throw CustomException(ErrorCode.PROFILE_NOT_COMPLETED)
        }

        val course = courseRepository!!.findById(request.courseId)
            .orElseThrow<CustomException?>(Supplier { CustomException(ErrorCode.COURSE_NOT_FOUND) })
        val marathon = course.getMarathon()
        val now = LocalDateTime.now()

        // 접수 기간이 아니면 생성할 수 없다.
        if (now.isBefore(marathon.getRegistrationStartAt()) || now.isAfter(marathon.getRegistrationEndAt())) {
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
            user,
            course,
            marathon,
            request.snapZipCode,
            request.snapAddress,
            request.snapDetail,
            request.tSize,
            request.agreedTerms
        )

        val updatedRows = courseRepository.increaseCurrentCountIfNotFull(course.getId())
        // 코스 정원이 가득 찼으면 접수를 막는다.
        if (updatedRows == 0) {
            throw CustomException(ErrorCode.CAPACITY_FULL)
        }
        // 동시성 제어 미적용 메서드
        // course.increaseCurrentCount();
        val savedRegistration = registrationRepository!!.save<Registration>(registration)

        eventPublisher!!.publishEvent(
            RegistrationCompletedEvent(
                user.getEmail(),
                marathon.getTitle(),
                course.getCourseType()
            )
        )

        return from(savedRegistration)
    }

    fun cancel(userId: Long?, registrationId: Long) {
        val registration = registrationRepository!!.findById(registrationId) // 존재하지 않는 접수 건은 취소할 수 없다.
            .orElseThrow<CustomException?>(Supplier { CustomException(ErrorCode.REGISTRATION_NOT_FOUND) })

        // 본인 신청 건만 취소할 수 있다.
        if (registration.getUser().getId() != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val marathon = registration.getMarathon()

        val now = LocalDateTime.now()
        // 접수 마감 이후에는 취소할 수 없다.
        if (now.isAfter(marathon.getRegistrationEndAt())) {
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
        registrationCancelHistoryRepository!!.saveAndFlush<RegistrationCancelHistory?>(
            RegistrationCancelHistory.create(
                registration
            )
        )

        courseRepository!!.decreaseCurrentCountIfPositive(registration.getCourse().getId())
        // 동시성 제어 미적용 메서드
        // registration.getCourse().decreaseCurrentCount();
        registrationRepository.delete(registration)
    }

    // status 필터에 따른 내 접수 목록 조회
    // ACTIVE   : 정상 접수인 상태 (취소 되지 않은 모든 접수)
    // CANCELED : 취소된 접수 상태
    @Transactional(readOnly = true)
    fun getMyRegistrations(userId: Long?, status: MyRegistrationStatusFilter?, pageable: Pageable): MyRegistrationRes {
        if (status == MyRegistrationStatusFilter.CANCELED) {
            return getCanceledRegistrations(userId, pageable)
        }

        return getActiveRegistrations(userId, pageable)
    }

    private fun getActiveRegistrations(userId: Long?, pageable: Pageable): MyRegistrationRes {
        val sortedPageable: Pageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(
                Sort.Order.desc("appliedAt"),
                Sort.Order.desc("id")
            )
        )

        val page = registrationRepository!!.findByUser_Id(userId, sortedPageable)

        return MyRegistrationRes.fromActive(page)
    }

    private fun getCanceledRegistrations(userId: Long?, pageable: Pageable): MyRegistrationRes {
        val sortedPageable: Pageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(
                Sort.Order.desc("canceledAt"),
                Sort.Order.desc("id")
            )
        )

        val page = registrationCancelHistoryRepository!!.findByUserId(userId, sortedPageable)

        val marathonIds = page.getContent().stream()
            .map<Long?> { obj: RegistrationCancelHistory? -> obj!!.getMarathonId() }
            .distinct()
            .toList()

        val courseIds = page.getContent().stream()
            .map<Long?> { obj: RegistrationCancelHistory? -> obj!!.getCourseId() }
            .distinct()
            .toList()

        val marathonMap = marathonRepository!!.findAllById(marathonIds).stream()
            .collect(Collectors.toMap(Function { obj: Marathon? -> obj!!.getId() }, Function.identity<Marathon?>()))

        val courseMap = courseRepository!!.findAllById(courseIds).stream()
            .collect(Collectors.toMap(Function { obj: Course? -> obj!!.getId() }, Function.identity<Course?>()))

        return MyRegistrationRes.fromCanceled(page, marathonMap, courseMap)
    }
}
