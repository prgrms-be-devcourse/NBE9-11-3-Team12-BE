package com.rungo.api.domain.registration.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.course.repository.CourseRepository;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.registration.dto.RegistrationOverviewRes;
import com.rungo.api.domain.registration.dto.RegistrationParticipantDetailRes;
import com.rungo.api.domain.registration.dto.RegistrationParticipantListRes;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistrationOrganizerQueryService {

    private final RegistrationRepository registrationRepository;
    private final MarathonRepository marathonRepository;
    private final CourseRepository courseRepository;

    // 주최자 - 접수 요약 조회
    public RegistrationOverviewRes getRegistrationOverview(Long organizerId, Long marathonId) {

        Marathon marathon = getMarathonById(marathonId);
        validateOrganizer(marathon, organizerId);

        // 해당 마라톤의 코스 목록 조회
        List<Course> courses = courseRepository.findAllByMarathon_IdOrderByIdAsc(marathonId);

        return RegistrationOverviewRes.of(marathon, courses);
    }

    // 주최자 - 참가자 목록 조회
    public RegistrationParticipantListRes getMarathonParticipants(
            Long organizerId,
            Long marathonId,
            Long courseId,
            String name,
            Pageable pageable
    ) {

        Marathon marathon = getMarathonById(marathonId);
        validateOrganizer(marathon, organizerId);

        String keyword = normalizeKeyword(name);

        Page<Registration> page;

        // 필터 조합에 따라 참가자 목록 조회
        if (courseId == null && keyword == null) {
            page = registrationRepository.findByMarathon_Id(marathonId, pageable);
        } else if (courseId != null && keyword == null) {
            page = registrationRepository.findByMarathon_IdAndCourse_Id(marathonId, courseId, pageable);
        } else if (courseId == null) {
            page = registrationRepository.findByMarathon_IdAndSnapNameContaining(marathonId, keyword, pageable);
        } else {
            page = registrationRepository.findByMarathon_IdAndCourse_IdAndSnapNameContaining(
                    marathonId, courseId, keyword, pageable
            );
        }

        return RegistrationParticipantListRes.from(page);
    }

    // 주최자 - 참가자 상세 조회
    public RegistrationParticipantDetailRes getMarathonParticipantDetail(
            Long organizerId,
            Long marathonId,
            Long registrationId
    ) {

        Marathon marathon = getMarathonById(marathonId);
        validateOrganizer(marathon, organizerId);

        // 접수 여부 검증
        Registration registration = registrationRepository.findByIdAndMarathon_Id(registrationId, marathonId)
                .orElseThrow(() -> new CustomException(ErrorCode.REGISTRATION_NOT_FOUND));

        return RegistrationParticipantDetailRes.from(registration);
    }

    // 마라톤 존재 여부 검증
    private Marathon getMarathonById(Long marathonId) {
        return marathonRepository.findById(marathonId)
                .orElseThrow(() -> new CustomException(ErrorCode.MARATHON_NOT_FOUND));
    }

    // 주최자 일치 여부 검증
    private void validateOrganizer(Marathon marathon, Long organizerId) {
        if (!marathon.getOrganizer().getId().equals(organizerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    // 검색 이름 정리 -> null/blank는 검색 조건 없음으로 간주, 공백 검색 방지
    private String normalizeKeyword(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }
}
