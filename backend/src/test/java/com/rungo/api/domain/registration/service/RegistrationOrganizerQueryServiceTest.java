package com.rungo.api.domain.registration.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.course.repository.CourseRepository;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.registration.dto.RegistrationOverviewRes;
import com.rungo.api.domain.registration.dto.RegistrationParticipantDetailRes;
import com.rungo.api.domain.registration.dto.RegistrationParticipantListRes;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.enumtype.RegistrationStatus;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationOrganizerQueryServiceTest {

    @InjectMocks
    private RegistrationOrganizerQueryService registrationOrganizerQueryService;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private MarathonRepository marathonRepository;

    @Mock
    private CourseRepository courseRepository;

    @Test
    @DisplayName("접수 요약 조회 성공 - 마라톤 정보와 코스별 집계가 정상 반환된다")
    void getRegistrationOverview_success() {
        Long organizerId = 1L;
        Long marathonId = 10L;

        Users organizer = createUser(organizerId, "주최자", "010-9999-9999", Role.ORGANIZER);
        Marathon marathon = createMarathon(marathonId, organizer);

        Course course1 = createCourse(101L, marathon, "10K", 50000, 100, 25);
        Course course2 = createCourse(102L, marathon, "Half", 70000, 200, 60);

        given(marathonRepository.findById(marathonId)).willReturn(Optional.of(marathon));
        given(courseRepository.findAllByMarathon_IdOrderByIdAsc(marathonId))
                .willReturn(List.of(course1, course2));

        RegistrationOverviewRes result =
                registrationOrganizerQueryService.getRegistrationOverview(organizerId, marathonId);

        assertNotNull(result);
        assertEquals(10L, result.marathon().marathonId());
        assertEquals("서울 마라톤", result.marathon().marathonTitle());
        assertEquals(LocalDate.of(2026, 10, 25), result.marathon().eventDate());
        assertEquals("서울", result.marathon().region());
        assertEquals(85, result.marathon().totalCurrentCount());
        assertEquals(300, result.marathon().totalCapacity());
        assertEquals(215, result.marathon().totalRemainingCount());
        assertEquals(28, result.marathon().totalRecruitmentRate());

        assertEquals(2, result.courseStatuses().size());

        RegistrationOverviewRes.CourseStatus first = result.courseStatuses().get(0);
        assertEquals(101L, first.courseId());
        assertEquals("10K", first.courseType());
        assertEquals(new BigDecimal("50000"), first.price());
        assertEquals(25, first.currentCount());
        assertEquals(100, first.capacity());
        assertEquals(75, first.remainingCount());
        assertEquals(25, first.recruitmentRate());

        RegistrationOverviewRes.CourseStatus second = result.courseStatuses().get(1);
        assertEquals(102L, second.courseId());
        assertEquals("Half", second.courseType());
        assertEquals(new BigDecimal("70000"), second.price());
        assertEquals(60, second.currentCount());
        assertEquals(200, second.capacity());
        assertEquals(140, second.remainingCount());
        assertEquals(30, second.recruitmentRate());
    }

    @Test
    @DisplayName("참가자 목록 조회 성공 - courseId와 name이 함께 오면 복합 필터 조회를 수행한다")
    void getMarathonParticipants_success_with_courseId_and_name() {
        Long organizerId = 1L;
        Long marathonId = 10L;
        Long courseId = 101L;
        String name = "  홍길동  ";

        Pageable pageable = PageRequest.of(0, 20, Sort.by(
                Sort.Order.desc("appliedAt"),
                Sort.Order.desc("id")
        ));

        Users organizer = createUser(organizerId, "주최자", "010-9999-9999", Role.ORGANIZER);
        Users participant = createUser(2L, "홍길동", "010-1111-2222", Role.PARTICIPANT);
        Marathon marathon = createMarathon(marathonId, organizer);
        Course course = createCourse(courseId, marathon, "10K", 50000, 100, 10);

        Registration registration = Registration.create(
                participant, course, marathon,
                "12345", "서울시 강남구", "101동", "L", true
        );
        ReflectionTestUtils.setField(registration, "id", 1000L);
        ReflectionTestUtils.setField(registration, "appliedAt", LocalDateTime.of(2026, 4, 20, 9, 30));

        Page<Registration> page = new PageImpl<>(List.of(registration), pageable, 1);

        given(marathonRepository.findById(marathonId)).willReturn(Optional.of(marathon));
        given(registrationRepository.findByMarathon_IdAndCourse_IdAndSnapNameContaining(
                marathonId, courseId, "홍길동", pageable
        )).willReturn(page);

        RegistrationParticipantListRes result = registrationOrganizerQueryService.getMarathonParticipants(
                organizerId, marathonId, courseId, name, pageable
        );

        verify(registrationRepository).findByMarathon_IdAndCourse_IdAndSnapNameContaining(
                marathonId, courseId, "홍길동", pageable
        );
        verify(registrationRepository, never()).findByMarathon_Id(anyLong(), any());
        verify(registrationRepository, never()).findByMarathon_IdAndCourse_Id(anyLong(), anyLong(), any());
        verify(registrationRepository, never()).findByMarathon_IdAndSnapNameContaining(anyLong(), anyString(), any());

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(0, result.pageRes().page());
        assertEquals(20, result.pageRes().size());
        assertEquals(1L, result.pageRes().totalElements());
        assertEquals(1, result.pageRes().totalPages());

        RegistrationParticipantListRes.Item item = result.content().get(0);
        assertEquals(1000L, item.registrationId());
        assertEquals("홍길동", item.name());
        assertEquals("010-1111-2222", item.phoneNumber());
        assertEquals("L", item.tSize());
        assertEquals(101L, item.courseId());
        assertEquals("10K", item.courseType());
        assertEquals(RegistrationStatus.COMPLETED, item.status());
        assertEquals(LocalDateTime.of(2026, 4, 20, 9, 30), item.appliedAt());
    }

    @Test
    @DisplayName("참가자 상세 조회 성공 - 접수 상세 정보가 정상 반환된다")
    void getMarathonParticipantDetail_success() {
        Long organizerId = 1L;
        Long marathonId = 10L;
        Long registrationId = 1000L;

        Users organizer = createUser(organizerId, "주최자", "010-9999-9999", Role.ORGANIZER);
        Users participant = createUser(2L, "홍길동", "010-1111-2222", Role.PARTICIPANT);
        Marathon marathon = createMarathon(marathonId, organizer);
        Course course = createCourse(101L, marathon, "10K", 50000, 100, 10);

        Registration registration = Registration.create(
                participant, course, marathon,
                "12345", "서울시 강남구", "101동", "L", true
        );
        ReflectionTestUtils.setField(registration, "id", registrationId);
        ReflectionTestUtils.setField(registration, "appliedAt", LocalDateTime.of(2026, 4, 20, 9, 30));

        given(marathonRepository.findById(marathonId)).willReturn(Optional.of(marathon));
        given(registrationRepository.findByIdAndMarathon_Id(registrationId, marathonId))
                .willReturn(Optional.of(registration));

        RegistrationParticipantDetailRes result =
                registrationOrganizerQueryService.getMarathonParticipantDetail(
                        organizerId, marathonId, registrationId
                );

        assertNotNull(result);
        assertEquals(1000L, result.registrationId());
        assertEquals(10L, result.marathonId());
        assertEquals("서울 마라톤", result.marathonTitle());
        assertEquals(101L, result.courseId());
        assertEquals("10K", result.courseType());
        assertEquals(RegistrationStatus.COMPLETED, result.status());
        assertEquals("홍길동", result.snapName());
        assertEquals("010-1111-2222", result.snapPhoneNumber());
        assertEquals("12345", result.snapZipCode());
        assertEquals("서울시 강남구", result.snapAddress());
        assertEquals("101동", result.snapDetail());
        assertEquals("L", result.tSize());
        assertTrue(result.agreedTerms());
        assertEquals(LocalDateTime.of(2026, 4, 20, 9, 30), result.appliedAt());
    }

    @Test
    @DisplayName("접수 요약 조회 실패 - 마라톤이 없으면 MARATHON_NOT_FOUND 예외를 던진다")
    void getRegistrationOverview_fail_marathon_not_found() {
        Long organizerId = 1L;
        Long marathonId = 999L;

        given(marathonRepository.findById(marathonId)).willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationOrganizerQueryService.getRegistrationOverview(organizerId, marathonId)
        );

        assertEquals(ErrorCode.MARATHON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("참가자 목록 조회 실패 - 주최자가 아니면 FORBIDDEN 예외를 던진다")
    void getMarathonParticipants_fail_forbidden() {
        Long organizerId = 1L;
        Long otherOrganizerId = 99L;
        Long marathonId = 10L;
        Pageable pageable = PageRequest.of(0, 20);

        Users realOrganizer = createUser(otherOrganizerId, "다른주최자", "010-9999-8888", Role.ORGANIZER);
        Marathon marathon = createMarathon(marathonId, realOrganizer);

        given(marathonRepository.findById(marathonId)).willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationOrganizerQueryService.getMarathonParticipants(
                        organizerId, marathonId, null, null, pageable
                )
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(registrationRepository);
    }

    @Test
    @DisplayName("참가자 상세 조회 실패 - 접수가 없으면 REGISTRATION_NOT_FOUND 예외를 던진다")
    void getMarathonParticipantDetail_fail_registration_not_found() {
        Long organizerId = 1L;
        Long marathonId = 10L;
        Long registrationId = 999L;

        Users organizer = createUser(organizerId, "주최자", "010-9999-9999", Role.ORGANIZER);
        Marathon marathon = createMarathon(marathonId, organizer);

        given(marathonRepository.findById(marathonId)).willReturn(Optional.of(marathon));
        given(registrationRepository.findByIdAndMarathon_Id(registrationId, marathonId))
                .willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationOrganizerQueryService.getMarathonParticipantDetail(
                        organizerId, marathonId, registrationId
                )
        );

        assertEquals(ErrorCode.REGISTRATION_NOT_FOUND, exception.getErrorCode());
    }

    private Users createUser(Long id, String name, String phoneNumber, Role role) {
        Users user = Users.builder()
                .email(name + "@test.com")
                .name(name)
                .phoneNumber(phoneNumber)
                .role(role)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1999, 1, 1))
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Marathon createMarathon(Long marathonId, Users organizer) {
        Marathon marathon = new Marathon(
                organizer,
                "서울 마라톤",
                "서울",
                "잠실종합운동장",
                LocalDate.of(2026, 10, 25),
                "poster.png",
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 9, 30, 23, 59),
                MarathonStatus.OPEN
        );
        ReflectionTestUtils.setField(marathon, "id", marathonId);
        return marathon;
    }

    private Course createCourse(Long courseId, Marathon marathon, String courseType, int price, int capacity, int currentCount) {
        Course course = new Course(courseType, BigDecimal.valueOf(price), capacity, currentCount);
        course.setMarathon(marathon);
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }
}