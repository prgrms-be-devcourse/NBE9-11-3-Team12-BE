package com.rungo.api.domain.registration.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.course.repository.CourseRepository;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent;
import com.rungo.api.domain.registration.dto.CreateRegistrationReq;
import com.rungo.api.domain.registration.dto.CreateRegistrationRes;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory;
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistrationCommandServiceTest {

    @InjectMocks
    private RegistrationService registrationService;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RegistrationCancelHistoryRepository registrationCancelHistoryRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("접수 생성 성공 - 저장과 응답 반환 및 코스 인원 증가가 정상 동작한다")
    void create_success() {
        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                MarathonStatus.OPEN
        );
        ReflectionTestUtils.setField(marathon, "id", 2L);

        Course course = createCourse(marathon, 100, 10);
        ReflectionTestUtils.setField(course, "id", 3L);

        CreateRegistrationReq request = new CreateRegistrationReq(3L, "12345", "서울시 강남구", "101동", "L", true);
        Registration savedRegistration = Registration.create(
                user,
                course,
                marathon,
                request.snapZipCode(),
                request.snapAddress(),
                request.snapDetail(),
                request.tSize(),
                request.agreedTerms()
        );
        LocalDateTime appliedAt = LocalDateTime.of(2026, 4, 15, 10, 0);
        ReflectionTestUtils.setField(savedRegistration, "id", 4L);
        ReflectionTestUtils.setField(savedRegistration, "appliedAt", appliedAt);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(courseRepository.findById(3L)).willReturn(Optional.of(course));
        given(courseRepository.increaseCurrentCountIfNotFull(3L)).willReturn(1);
        given(registrationRepository.save(any(Registration.class))).willReturn(savedRegistration);

        CreateRegistrationRes result = registrationService.create(1L, request);

        ArgumentCaptor<Registration> registrationCaptor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository, times(1)).save(registrationCaptor.capture());

        Registration capturedRegistration = registrationCaptor.getValue();
        assertSame(user, capturedRegistration.getUser());
        assertSame(course, capturedRegistration.getCourse());
        assertSame(marathon, capturedRegistration.getMarathon());
        assertEquals("COMPLETED", capturedRegistration.getStatus().name());
        assertEquals("홍길동", capturedRegistration.getSnapName());
        assertEquals("010-1111-2222", capturedRegistration.getSnapPhoneNumber());
        assertEquals("12345", capturedRegistration.getSnapZipCode());
        assertEquals("서울시 강남구", capturedRegistration.getSnapAddress());
        assertEquals("101동", capturedRegistration.getSnapDetail());
        assertEquals("L", capturedRegistration.getTSize());
        assertEquals(true, capturedRegistration.isAgreedTerms());

        assertNotNull(result);
        assertEquals(4L, result.registrationId());
        assertEquals(2L, result.marathonId());
        assertEquals("서울 마라톤", result.marathonTitle());
        assertEquals(3L, result.courseId());
        assertEquals("10K", result.courseType());
        assertEquals("COMPLETED", result.status());
        assertEquals(appliedAt, result.appliedAt());

        verify(courseRepository, times(1)).increaseCurrentCountIfNotFull(3L);
        verify(eventPublisher, times(1)).publishEvent(any(RegistrationCompletedEvent.class));
    }

    @Test
    @DisplayName("접수 생성 실패 - 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void create_fail_user_not_found() {
        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", true);
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.create(1L, request)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 생성 실패 - 약관에 동의하지 않으면 REGISTRATION_TERMS_REQUIRED 예외가 발생한다")
    void create_fail_terms_required() {
        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", false);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.create(1L, request)
        );

        assertEquals(ErrorCode.REGISTRATION_TERMS_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 생성 실패 - 코스가 없으면 COURSE_NOT_FOUND 예외가 발생한다")
    void create_fail_course_not_found() {
        Users user = createUser(1L, "홍길동", "010-1111-2222");
        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", true);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(courseRepository.findById(1L)).willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.create(1L, request)
        );

        assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 생성 실패 - 접수 시작 전이면 REGISTRATION_PERIOD_INVALID 예외가 발생한다")
    void create_fail_before_registration_start() {
        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                MarathonStatus.OPEN
        );
        Course course = createCourse(marathon, 100, 10);
        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", true);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.create(1L, request)
        );

        assertEquals(ErrorCode.REGISTRATION_PERIOD_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 생성 실패 - 접수 종료 후이면 REGISTRATION_PERIOD_INVALID 예외가 발생한다")
    void create_fail_after_registration_end() {
        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon(
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                MarathonStatus.OPEN
        );
        Course course = createCourse(marathon, 100, 10);
        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", true);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.create(1L, request)
        );

        assertEquals(ErrorCode.REGISTRATION_PERIOD_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 생성 실패 - 취소된 대회이면 MARATHON_ALREADY_CANCELED 예외가 발생한다")
    void create_fail_marathon_not_open() {
        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                MarathonStatus.CANCELED
        );
        Course course = createCourse(marathon, 100, 10);
        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", true);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.create(1L, request)
        );

        assertEquals(ErrorCode.MARATHON_ALREADY_CANCELED, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 생성 실패 - 코스 정원이 가득 차면 CAPACITY_FULL 예외가 발생한다")
    void create_fail_capacity_full() {
        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                MarathonStatus.OPEN
        );
        Course course = createCourse(marathon, 10, 10);
        ReflectionTestUtils.setField(course, "id", 1L);
        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", true);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(courseRepository.increaseCurrentCountIfNotFull(1L)).willReturn(0);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.create(1L, request)
        );

        assertEquals(ErrorCode.CAPACITY_FULL, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 취소 성공 - 삭제와 코스 인원 감소가 정상 동작한다")
    void cancel_success() {
        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                MarathonStatus.OPEN
        );
        Course course = createCourse(marathon, 100, 10);
        ReflectionTestUtils.setField(course, "id", 3L);
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

        given(registrationRepository.findById(1L)).willReturn(Optional.of(registration));

        registrationService.cancel(1L, 1L);

        verify(registrationCancelHistoryRepository, times(1)).saveAndFlush(any(RegistrationCancelHistory.class));
        verify(courseRepository, times(1)).decreaseCurrentCountIfPositive(3L);
        verify(registrationRepository, times(1)).delete(registration);
    }

    @Test
    @DisplayName("접수 취소 실패 - 접수 내역이 없으면 REGISTRATION_NOT_FOUND 예외가 발생한다")
    void cancel_fail_registration_not_found() {
        given(registrationRepository.findById(1L)).willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.cancel(1L, 1L)
        );

        assertEquals(ErrorCode.REGISTRATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 취소 실패 - 본인 접수 건이 아니면 FORBIDDEN 예외가 발생한다")
    void cancel_fail_forbidden() {
        Users user = createUser(2L, "김철수", "010-2222-3333");
        Marathon marathon = createMarathon(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                MarathonStatus.OPEN
        );
        Course course = createCourse(marathon, 100, 10);
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

        given(registrationRepository.findById(1L)).willReturn(Optional.of(registration));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.cancel(1L, 1L)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 취소 실패 - 접수 마감 이후면 REGISTRATION_CANCEL_PERIOD_INVALID 예외가 발생한다")
    void cancel_fail_after_registration_end() {
        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon(
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                MarathonStatus.OPEN
        );
        Course course = createCourse(marathon, 100, 10);
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

        given(registrationRepository.findById(1L)).willReturn(Optional.of(registration));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.cancel(1L, 1L)
        );

        assertEquals(ErrorCode.REGISTRATION_CANCEL_PERIOD_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("접수 취소 실패 - 취소된 대회이면 MARATHON_ALREADY_CANCELED 예외가 발생한다")
    void cancel_fail_marathon_not_open() {
        Users user = createUser(1L, "홍길동", "010-1111-2222");
        Marathon marathon = createMarathon(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                MarathonStatus.CANCELED
        );
        Course course = createCourse(marathon, 100, 10);
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

        given(registrationRepository.findById(1L)).willReturn(Optional.of(registration));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> registrationService.cancel(1L, 1L)
        );

        assertEquals(ErrorCode.MARATHON_ALREADY_CANCELED, exception.getErrorCode());
    }

    private Users createUser(Long id, String name, String phoneNumber) {
        return Users.builder()
                .id(id)
                .email("test@test.com")
                .name(name)
                .phoneNumber(phoneNumber)
                .role(Role.PARTICIPANT)
                .gender(Gender.MALE)
                .birth(LocalDate.of(2000, 1, 1))
                .build();
    }

    private Marathon createMarathon(
            LocalDateTime registrationStartAt,
            LocalDateTime registrationEndAt,
            MarathonStatus status
    ) {
        return new Marathon(
                createUser(99L, "주최자", "010-9999-9999"),
                "서울 마라톤",
                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),
                "poster.png",
                registrationStartAt,
                registrationEndAt,
                status
        );
    }

    private Course createCourse(Marathon marathon, int capacity, int currentCount) {
        Course course = new Course("10K", BigDecimal.valueOf(30000), capacity, currentCount);
        marathon.addCourse(course);
        return course;
    }
}
