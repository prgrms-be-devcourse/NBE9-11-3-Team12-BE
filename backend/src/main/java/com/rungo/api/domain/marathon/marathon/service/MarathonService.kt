package com.rungo.api.domain.marathon.marathon.service

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonReq
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.delete.CancelMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonDetailRes
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonListRes
import com.rungo.api.domain.marathon.marathon.dto.read.ReadMyMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonReq
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonRes
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.notification.event.MarathonCanceledEvent
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import com.rungo.api.domain.registration.enumtype.RegistrationCancelReason
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.file.FileStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class MarathonService(
    private val marathonRepository: MarathonRepository,
    private val userRepository: UserRepository,
    private val registrationRepository: RegistrationRepository,
    private val registrationCancelHistoryRepository: RegistrationCancelHistoryRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val fileStorageService: FileStorageService,
    @Value("\${marathon.min-days.start-to-end}")
    private val minDaysBetweenStartAndEnd: Long,
    @Value("\${marathon.min-days.end-to-event}")
    private val minDaysBetweenEndAndEvent: Long,

) {


    @Transactional
    fun createMarathon(id: Long, req: CreateMarathonReq): CreateMarathonRes {
        val organizer = findOrganizer(id)

        val posterImageUrl = req.posterImage?.let {
            fileStorageService.saveMarathonPoster(it)
        }

        validateMarathonSchedule(
            req.registrationStartAt,
            req.registrationEndAt,
            req.eventDate
        )

        //코스 타입 중복이면 예외 처리
        val normalizedCourseTypes = req.courses
            .map { normalizeCourseType(it.courseType) }
        if (normalizedCourseTypes.size != normalizedCourseTypes.toSet().size) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }
        val marathon = Marathon.create(
            organizer,
            req.title,
            req.region,
            req.detailedAddress,
            req.eventDate,
            posterImageUrl,
            req.registrationStartAt,
            req.registrationEndAt
        )

        req.courses.forEach { courseReq ->
            marathon.addCourse(
                Course.create(
                    normalizeCourseType(courseReq.courseType),
                    courseReq.price,
                    courseReq.capacity,
                    0
                )
            )
        }
        val savedMarathon = marathonRepository.save(marathon)

        return CreateMarathonRes.from(savedMarathon)
    }

    @Transactional(readOnly = true)
    fun getMarathonDetail(marathonId: Long): MarathonDetailRes {
        val marathon = getMarathonOrThrow(marathonId)
        if (marathon.status in setOf(MarathonStatus.CANCELED, MarathonStatus.CANCELING)) {
            throw CustomException(ErrorCode.MARATHON_CANCELED)
        }
        return MarathonDetailRes.from(marathon)
    }

    @Transactional(readOnly = true)
    fun getMarathons(pageable: Pageable): MarathonListRes {
        val page = marathonRepository.findByStatusIn(
            listOf(MarathonStatus.TEMP, MarathonStatus.OPEN),
            pageable,
        )
        return MarathonListRes.from(page)
    }


    @Transactional(readOnly = true)
    fun getMyMarathons(userId: Long): List<ReadMyMarathonRes> {
        findOrganizer(userId) // 주최자 존재 여부 및 권한 체크

        val marathons = marathonRepository.findByOrganizerIdAndStatusNotIn(
            userId,
            listOf(MarathonStatus.CANCELING, MarathonStatus.CANCELED),
        )

        return marathons.map(ReadMyMarathonRes::from)
    }


    @Transactional
    fun cancelMarathon(id: Long, marathonId: Long): CancelMarathonRes {
        val organizer = findOrganizer(id)
        val marathon = getMarathonOrThrow(marathonId)

        //자기 자신이 신청한 마라톤만 취소할 수 있도록 예외 처리
        if (marathon.organizer.id != organizer.id) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        // 참가자 이메일 미리 조회 (N+1 방지용 JPQL 활용)
        val participantEmails = registrationRepository.findParticipantEmailsByMarathonId(marathonId)
        val registrations = registrationRepository.findAllByMarathon_IdOrderByAppliedAtDesc(marathonId)

        marathon.cancel()

        if (registrations.isNotEmpty()) {
            val cancelHistories = registrations.map { registration ->
                RegistrationCancelHistory.create(
                    registration,
                    RegistrationCancelReason.MARATHON_CANCELED,
                )
            }

            registrationCancelHistoryRepository.saveAll(cancelHistories)
            registrationRepository.deleteAll(registrations)
        }

        marathon.courses.forEach { course ->
            course.resetCurrentCount()
        }

        // 참가자 있을 경우만 이벤트 발행
        if (participantEmails.isNotEmpty()) {
            eventPublisher.publishEvent(
                MarathonCanceledEvent(
                    marathon.title,
                    participantEmails,
                )
            )
        }

        return CancelMarathonRes.from(marathon)
    }

    @Transactional
    fun updateMarathon(organizerId: Long, marathonId: Long, req: UpdateMarathonReq): UpdateMarathonRes {
        val marathon = marathonRepository.findByIdAndOrganizer_Id(marathonId, organizerId)
            .orElseThrow { CustomException(ErrorCode.MARATHON_NOT_FOUND) }

        //마라톤 접수 전까지만 수정 가능하도록 예외 처리
        if (!LocalDateTime.now().isBefore(marathon.registrationStartAt)) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        if (marathon.isCanceled()) {
            throw CustomException(ErrorCode.MARATHON_ALREADY_CANCELED)
        }

        val registrationStartAt = req.registrationStartAt ?: marathon.registrationStartAt
        val registrationEndAt = req.registrationEndAt ?: marathon.registrationEndAt
        val eventDate = req.eventDate ?: marathon.eventDate

        validateMarathonSchedule(
            registrationStartAt,
            registrationEndAt,
            eventDate
        )

        val posterImageUrl = fileStorageService.saveMarathonPoster(req.posterImage)

        //기존에 있는 Course를 Map으로 저장
        val courseMap = toCourseMap(marathon)

        validateDuplicateCourseIds(req)
        validateDuplicateCourseType(req, marathon, courseMap)


        marathon.updateMarathonInfo(
            req.title,
            req.region,
            req.detailedAddress,
            req.eventDate,
            posterImageUrl,
            req.registrationStartAt,
            req.registrationEndAt
        )

        //courses 가 NUll 이 아니라면 코스 수정 로직 수행, NULL 이면 코스 수정 없이 마라톤 정보만 업데이트
        req.courses?.forEach { courseReq ->
            val course = courseMap[courseReq.id]
                ?: throw CustomException(ErrorCode.COURSE_NOT_FOUND)

            course.updateCourseInfo(
                courseReq.courseType?.let(::normalizeCourseType),
                courseReq.price,
                courseReq.capacity,
            )
        }

        return UpdateMarathonRes.from(marathon)
    }


    // 5k -> 5K, 10k -> 10K, " 5k " -> 5K 로 저장하기 위해 정규화하는 함수
    private fun normalizeCourseType(courseType: String): String =
        courseType.trim().uppercase(Locale.getDefault())

    private fun getMarathonOrThrow(marathonId: Long): Marathon {
        return marathonRepository.findByIdOrNull(marathonId)
            ?: throw CustomException(ErrorCode.MARATHON_NOT_FOUND)
    }

    //id로 주최자 조회 함수, 존재하지 않거나 주최자가 아니면 예외 처리
    private fun findOrganizer(id: Long): Users {
        // 주최하는 사람이 존재하는지 확인
        val organizer = userRepository.findByIdOrNull(id)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        // 주최자 측 인가 확인
        if (organizer.role != Role.ORGANIZER) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
        return organizer
    }


    //코스 중복 여부 검사하는 함수
    private fun validateDuplicateCourseType(
        req: UpdateMarathonReq,
        marathon: Marathon,
        courseMap: Map<Long, Course>
    ) {
        // 수정 요청에 코스가 없으면 검증 X

        if (req.courses.isNullOrEmpty()) {
            return
        }

        // 먼저 기존 Course를 중복허용 하는 Map으로 저장.
        val finalCourseTypes = marathon.courses
            .associate { course ->
                course.id to normalizeCourseType(course.courseType)
            }
            .toMutableMap()

        //수정 사항 반영.
        req.courses.forEach { courseReq ->
            val target = courseMap[courseReq.id]
                ?: throw CustomException(ErrorCode.COURSE_NOT_FOUND)

            courseReq.courseType?.let { courseType ->
                finalCourseTypes[target.id] = normalizeCourseType(courseType)
            }
        }

        //Set에 기존에 만들었던 Map의 Value를 저장.
        // 이때 중복이 생긴다면, Set의 사이즈와 Map의 사이즈가 달라지는 것을 이용
        val uniqueTypes = finalCourseTypes.values.toSet()

        if (uniqueTypes.size != finalCourseTypes.size) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }
    }

    //코스 아이디 중복 여부 검사하는 함수.
    private fun validateDuplicateCourseIds(req: UpdateMarathonReq) {
        val courses = req.courses.orEmpty()

        if (courses.isEmpty()) {
            return
        }

        val courseIds = courses.map { it.id }

        if (courseIds.size != courseIds.toSet().size) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }
    }

    //기존 코스 리스트를 courseId를 key로 하는 Map으로 변환하여, 업데이트 요청에서 코스 아이디로 기존 코스 정보를 빠르게 조회할 수 있도록 함
    private fun toCourseMap(marathon: Marathon): Map<Long, Course> =
        marathon.courses.associateBy { it.id }


    //간격 사이에 최소 값 유효성 검사.
    private fun validateMarathonSchedule(
        registrationStartAt: LocalDateTime,
        registrationEndAt: LocalDateTime,
        eventDate: LocalDate
    ) {

        // 대회 접수 시작일이 종료일보다 이후이면 예외 처리
        if (registrationStartAt.isAfter(registrationEndAt)) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        // 대회 개최일이 종료일 보다 이전이면 예외 처리
        if (eventDate.isBefore(registrationEndAt.toLocalDate())) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }
        val daysBetweenStartAndEnd =
            Duration.between(registrationStartAt, registrationEndAt).toDays()

        if (daysBetweenStartAndEnd < minDaysBetweenStartAndEnd) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        val daysBetweenEndAndEvent =
            Duration.between(registrationEndAt, eventDate.atStartOfDay()).toDays()

        if (daysBetweenEndAndEvent < minDaysBetweenEndAndEvent) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }
    }

    private fun activateIfStarted(marathon: Marathon) {
        if (marathon.status == MarathonStatus.TEMP
            && !LocalDateTime.now().isBefore(marathon.registrationStartAt)
        ) {
            marathon.open()
        }
    }
}
