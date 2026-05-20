package com.rungo.api.domain.registration.service

import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.registration.dto.RegistrationOverviewRes
import com.rungo.api.domain.registration.dto.RegistrationParticipantDetailRes
import com.rungo.api.domain.registration.dto.RegistrationParticipantListRes
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RegistrationOrganizerQueryService(
    private val registrationRepository: RegistrationRepository,
    private val marathonRepository: MarathonRepository,
    private val courseRepository: CourseRepository,
) {
    // 주최자 - 접수 요약 조회
    fun getRegistrationOverview(
        organizerId: Long,
        marathonId: Long,
    ): RegistrationOverviewRes {
        val marathon = getMarathonById(marathonId)
        validateOrganizer(marathon, organizerId)

        // 해당 마라톤의 코스 목록 조회
        val courses = courseRepository.findAllByMarathon_IdOrderByIdAsc(marathonId)

        return RegistrationOverviewRes.of(marathon, courses)
    }

    // 주최자 - 참가자 목록 조회
    fun getMarathonParticipants(
        organizerId: Long,
        marathonId: Long,
        courseId: Long?,
        name: String?,
        pageable: Pageable,
    ): RegistrationParticipantListRes {
        val marathon = getMarathonById(marathonId)
        validateOrganizer(marathon, organizerId)

        val keyword = normalizeKeyword(name)

        // 필터 조합에 따라 참가자 목록 조회
        val page = when {
            courseId == null && keyword == null ->
                registrationRepository.findByMarathon_Id(marathonId, pageable)
            courseId != null && keyword == null ->
                registrationRepository.findByMarathon_IdAndCourse_Id(marathonId, courseId, pageable)
            courseId == null && keyword != null ->
                registrationRepository.findByMarathon_IdAndSnapNameContaining(marathonId, keyword, pageable)
            else ->
                registrationRepository.findByMarathon_IdAndCourse_IdAndSnapNameContaining(marathonId, courseId!!, keyword!!, pageable)
        }


        return RegistrationParticipantListRes.from(page)
    }

    // 주최자 - 참가자 상세 조회
    fun getMarathonParticipantDetail(
        organizerId: Long,
        marathonId: Long,
        registrationId: Long,
    ): RegistrationParticipantDetailRes {
        val marathon = getMarathonById(marathonId)
        validateOrganizer(marathon, organizerId)

        // 마라톤에 속한 접수인지 검증
        val registration = registrationRepository.findByIdAndMarathon_Id(registrationId, marathonId)
            ?: throw CustomException(ErrorCode.REGISTRATION_NOT_FOUND)

        return RegistrationParticipantDetailRes.from(registration)
    }

    // 마라톤 존재 여부 검증
    private fun getMarathonById(marathonId: Long): Marathon = marathonRepository.findByIdOrNull(marathonId)
            ?: throw CustomException(ErrorCode.MARATHON_NOT_FOUND)

    // 주최자 일치 여부 검증
    private fun validateOrganizer(marathon: Marathon, organizerId: Long) {
        if (marathon.organizer.id != organizerId) throw CustomException(ErrorCode.FORBIDDEN)
    }

    // 검색 이름 정리 -> null/blank는 검색 조건 없음으로 간주, 공백 검색 방지
    private fun normalizeKeyword(name: String?): String? =
        name?.trim()?.takeIf { it.isNotBlank() }
}