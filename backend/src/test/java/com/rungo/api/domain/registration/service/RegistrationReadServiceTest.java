package com.rungo.api.domain.registration.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.course.repository.CourseRepository;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.registration.dto.MyRegistrationRes;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory;
import com.rungo.api.domain.registration.enumtype.MyRegistrationStatusFilter;
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistrationReadServiceTest {

    @InjectMocks
    private RegistrationService registrationService;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RegistrationCancelHistoryRepository registrationCancelHistoryRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MarathonRepository marathonRepository;

    @Test
    @DisplayName("내 접수 조회 성공 - ACTIVE 조회 시 appliedAt, id 내림차순으로 정상 접수 목록을 반환한다")
    void getMyRegistrations_active_success() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon();
        ReflectionTestUtils.setField(marathon, "id", 10L);

        Course course = createCourse(marathon, "10K", 50000, 100, 10);
        ReflectionTestUtils.setField(course, "id", 20L);

        Registration registration = Registration.create(
                user,
                course,
                marathon,
                "12345",
                "서울시 강남구",
                "101동",
                "L",
                true
        );
        ReflectionTestUtils.setField(registration, "id", 30L);
        ReflectionTestUtils.setField(registration, "appliedAt", LocalDateTime.of(2026, 4, 20, 9, 30));

        Page<Registration> page = new PageImpl<>(List.of(registration), PageRequest.of(0, 20), 1);
        given(registrationRepository.findByUser_Id(org.mockito.ArgumentMatchers.eq(userId), any(Pageable.class)))
                .willReturn(page);

        MyRegistrationRes result = registrationService.getMyRegistrations(userId, MyRegistrationStatusFilter.ACTIVE, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(registrationRepository).findByUser_Id(org.mockito.ArgumentMatchers.eq(userId), pageableCaptor.capture());
        verify(registrationCancelHistoryRepository, never()).findByUserId(any(), any());

        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
        assertEquals(20, captured.getPageSize());
        assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("appliedAt").getDirection());
        assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("id").getDirection());

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(0, result.pageRes().page());
        assertEquals(20, result.pageRes().size());
        assertEquals(1L, result.pageRes().totalElements());
        assertEquals(1, result.pageRes().totalPages());

        MyRegistrationRes.Item item = result.content().get(0);
        assertEquals(30L, item.registrationId());
        assertNull(item.historyId());
        assertEquals(10L, item.marathonId());
        assertEquals("서울 마라톤", item.marathonTitle());
        assertEquals(20L, item.courseId());
        assertEquals("10K", item.courseType());
        assertEquals("ACTIVE", item.status());
        assertEquals(new BigDecimal("50000"), item.price());
        assertEquals(LocalDate.of(2026, 10, 25), item.eventDate());
        assertEquals("홍길동", item.snapName());
        assertEquals("010-1111-2222", item.snapPhoneNumber());
        assertEquals("12345", item.snapZipCode());
        assertEquals("서울시 강남구", item.snapAddress());
        assertEquals("101동", item.snapDetail());
        assertEquals("L", item.tSize());
        assertEquals(true, item.agreedTerms());
        assertEquals(LocalDateTime.of(2026, 4, 20, 9, 30), item.appliedAt());
        assertNull(item.canceledAt());
    }

    @Test
    @DisplayName("내 접수 조회 성공 - CANCELED 조회 시 canceledAt, id 내림차순으로 취소 접수 목록을 반환한다")
    void getMyRegistrations_canceled_success() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(1, 10);

        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon();
        ReflectionTestUtils.setField(marathon, "id", 100L);

        Course course = createCourse(marathon, "Half", 70000, 200, 50);
        ReflectionTestUtils.setField(course, "id", 200L);

        Registration registration = Registration.create(
                user,
                course,
                marathon,
                "54321",
                "서울시 송파구",
                "202동",
                "M",
                true
        );
        ReflectionTestUtils.setField(registration, "id", 300L);
        ReflectionTestUtils.setField(registration, "appliedAt", LocalDateTime.of(2026, 4, 1, 8, 0));

        RegistrationCancelHistory history = RegistrationCancelHistory.create(registration);
        ReflectionTestUtils.setField(history, "id", 400L);
        ReflectionTestUtils.setField(history, "canceledAt", LocalDateTime.of(2026, 4, 5, 18, 30));

        Page<RegistrationCancelHistory> page = new PageImpl<>(List.of(history), PageRequest.of(1, 10), 11);

        given(registrationCancelHistoryRepository.findByUserId(org.mockito.ArgumentMatchers.eq(userId), any(Pageable.class)))
                .willReturn(page);
        given(marathonRepository.findAllById(List.of(100L))).willReturn(List.of(marathon));
        given(courseRepository.findAllById(List.of(200L))).willReturn(List.of(course));

        MyRegistrationRes result = registrationService.getMyRegistrations(userId, MyRegistrationStatusFilter.CANCELED, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(registrationCancelHistoryRepository)
                .findByUserId(org.mockito.ArgumentMatchers.eq(userId), pageableCaptor.capture());
        verify(registrationRepository, never()).findByUser_Id(any(), any());

        Pageable captured = pageableCaptor.getValue();
        assertEquals(1, captured.getPageNumber());
        assertEquals(10, captured.getPageSize());
        assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("canceledAt").getDirection());
        assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("id").getDirection());

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(1, result.pageRes().page());
        assertEquals(10, result.pageRes().size());
        assertEquals(11L, result.pageRes().totalElements());
        assertEquals(2, result.pageRes().totalPages());

        MyRegistrationRes.Item item = result.content().get(0);
        assertEquals(400L, item.registrationId());
        assertEquals(300L, item.historyId());
        assertEquals(100L, item.marathonId());
        assertEquals("서울 마라톤", item.marathonTitle());
        assertEquals(200L, item.courseId());
        assertEquals("Half", item.courseType());
        assertEquals("CANCELED", item.status());
        assertEquals(new BigDecimal("70000"), item.price());
        assertEquals(LocalDate.of(2026, 10, 25), item.eventDate());
        assertEquals("홍길동", item.snapName());
        assertEquals("010-1111-2222", item.snapPhoneNumber());
        assertEquals("54321", item.snapZipCode());
        assertEquals("서울시 송파구", item.snapAddress());
        assertEquals("202동", item.snapDetail());
        assertEquals("M", item.tSize());
        assertEquals(true, item.agreedTerms());
        assertEquals(LocalDateTime.of(2026, 4, 1, 8, 0), item.appliedAt());
        assertEquals(LocalDateTime.of(2026, 4, 5, 18, 30), item.canceledAt());
    }

    @Test
    @DisplayName("내 접수 조회 성공 - ACTIVE 조회 결과가 없으면 빈 목록과 빈 페이지 정보를 반환한다")
    void getMyRegistrations_active_empty() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        Page<Registration> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        given(registrationRepository.findByUser_Id(org.mockito.ArgumentMatchers.eq(userId), any(Pageable.class)))
                .willReturn(emptyPage);

        MyRegistrationRes result = registrationService.getMyRegistrations(userId, MyRegistrationStatusFilter.ACTIVE, pageable);

        assertNotNull(result);
        assertEquals(0, result.content().size());
        assertEquals(0, result.pageRes().page());
        assertEquals(20, result.pageRes().size());
        assertEquals(0L, result.pageRes().totalElements());
        assertEquals(0, result.pageRes().totalPages());
    }

    private Users createUser(Long id, String name, String phoneNumber) {
        Users user = Users.builder()
                .email("test@test.com")
                .name(name)
                .phoneNumber(phoneNumber)
                .role(Role.PARTICIPANT)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1999, 1, 1))
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Marathon createMarathon() {
        return new Marathon(
                createUser(99L, "주최자", "010-9999-9999"),
                "서울 마라톤",
                "서울",
                "잠실종합운동장",
                LocalDate.of(2026, 10, 25),
                "poster.png",
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 9, 30, 23, 59),
                MarathonStatus.OPEN
        );
    }

    private Course createCourse(Marathon marathon, String courseType, int price, int capacity, int currentCount) {
        Course course = new Course(courseType, BigDecimal.valueOf(price), capacity, currentCount);
        course.setMarathon(marathon);
        return course;
    }
}