package com.rungo.api.domain.registration.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.course.repository.CourseRepository;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.registration.dto.CreateRegistrationReq;
import com.rungo.api.domain.registration.dto.CreateRegistrationRes;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.domain.auth.repository.UserAuthRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.infrastructure.mail.EmailMessage;
import com.rungo.api.global.infrastructure.mail.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
class RegistrationCommandServiceIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Autowired
    private MarathonRepository marathonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @MockitoBean
    private EmailService emailService;

    @AfterEach
    void tearDown() {
        registrationRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        marathonRepository.deleteAllInBatch();
        userAuthRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("이메일 발송 실패가 발생해도 참가 접수 데이터는 정상 저장된다")
    void email_exception_isolation_test() {
        doThrow(new RuntimeException("SMTP 서버 강제 다운"))
                .when(emailService).send(any(EmailMessage.class));

        Users organizer = saveOrganizer("organizer@test.com");
        Users participant = saveParticipant("participant@test.com");
        Course course = saveCourseWithMarathon(organizer);

        CreateRegistrationReq req = createRegistrationReq(course.getId());

        CreateRegistrationRes res = registrationService.create(participant.getId(), req);

        assertThat(res).isNotNull();
        assertThat(res.registrationId()).isNotNull();
        assertThat(res.marathonTitle()).isEqualTo("서울 마라톤");
        assertThat(res.courseType()).isEqualTo("10K");
        assertThat(registrationRepository.findById(res.registrationId())).isPresent();

        Course savedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(savedCourse.getCurrentCount()).isEqualTo(1);

        verify(emailService, timeout(2000).atLeastOnce())
                .send(any(EmailMessage.class));
    }

    @Test
    @DisplayName("참가 접수 성공 시 이메일이 비동기로 발송되고 접수 데이터가 저장된다")
    void registration_success_email_send_test() {
        Users organizer = saveOrganizer("organizer-success@test.com");
        Users participant = saveParticipant("participant-success@test.com");
        Course course = saveCourseWithMarathon(organizer);

        CreateRegistrationReq req = createRegistrationReq(course.getId());

        CreateRegistrationRes res = registrationService.create(participant.getId(), req);

        assertThat(res).isNotNull();
        assertThat(res.registrationId()).isNotNull();
        assertThat(registrationRepository.findById(res.registrationId())).isPresent();

        Course savedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(savedCourse.getCurrentCount()).isEqualTo(1);

        verify(emailService, timeout(2000).times(1))
                .send(any(EmailMessage.class));
    }

    @Test
    @DisplayName("접수 생성 성공 시 Registration 저장과 currentCount 증가가 DB에 반영된다")
    void create_success_persists_registration_and_current_count() {
        Users organizer = saveOrganizer("organizer-db@test.com");
        Users participant = saveParticipant("participant-db@test.com");
        Marathon marathon = saveMarathon(organizer, "서울 마라톤");
        Course course = saveCourse(marathon, 10, 0);

        registrationService.create(participant.getId(), createRegistrationReq(course.getId()));

        assertEquals(1, registrationRepository.count());
        assertEquals(1, findCourse(course.getId()).getCurrentCount());
    }

    @Test
    @DisplayName("접수 취소 성공 시 Registration 삭제와 currentCount 감소가 DB에 반영된다")
    void cancel_success_deletes_registration_and_decreases_current_count() {
        Users organizer = saveOrganizer("organizer-cancel@test.com");
        Users participant = saveParticipant("participant-cancel@test.com");
        Marathon marathon = saveMarathon(organizer, "서울 마라톤");
        Course course = saveCourse(marathon, 10, 1);
        Registration registration = saveRegistration(participant, course, marathon);

        registrationService.cancel(participant.getId(), registration.getId());

        assertEquals(0, registrationRepository.count());
        assertEquals(0, findCourse(course.getId()).getCurrentCount());
    }

    @Test
    @DisplayName("동일 사용자가 같은 마라톤에 다시 신청하면 유니크 제약으로 실패한다")
    void create_fail_duplicate_registration() {
        Users organizer = saveOrganizer("organizer-duplicate@test.com");
        Users participant = saveParticipant("participant-duplicate@test.com");
        Marathon marathon = saveMarathon(organizer, "서울 마라톤");
        Course course = saveCourse(marathon, 10, 0);
        CreateRegistrationReq request = createRegistrationReq(course.getId());

        registrationService.create(participant.getId(), request);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> registrationService.create(participant.getId(), request)
        );

        assertEquals(1, registrationRepository.count());
        assertEquals(1, findCourse(course.getId()).getCurrentCount());
        assertTrue(containsConstraintName(exception, "uk_registration_user_marathon"));
    }

    private Users saveOrganizer(String email) {
        return userRepository.save(
                Users.builder()
                     .email(email)
                     .name("주최자")
                     .phoneNumber("010-1111-1111")
                     .role(Role.ORGANIZER)
                     .gender(Gender.MALE)
                     .birth(LocalDate.of(1990, 1, 1))
                     .build()
        );
    }

    private Users saveParticipant(String email) {
        return userRepository.save(
                Users.builder()
                     .email(email)
                     .name("참가자")
                     .phoneNumber("010-2222-2222")
                     .role(Role.PARTICIPANT)
                     .gender(Gender.MALE)
                     .birth(LocalDate.of(2000, 1, 1))
                     .build()
        );
    }

    private Course saveCourseWithMarathon(Users organizer) {
        Marathon marathon = saveMarathon(organizer, "서울 마라톤");
        return saveCourse(marathon, 100, 0);
    }

    private CreateRegistrationReq createRegistrationReq(Long courseId) {
        return new CreateRegistrationReq(
                courseId,
                "12345",
                "서울시 강남구",
                "101동",
                "L",
                true
        );
    }

    private Marathon saveMarathon(Users organizer, String title) {
        return marathonRepository.saveAndFlush(
                new Marathon(
                        organizer,
                        title,
                        "서울",
                        "성동구",
                        LocalDate.now().plusDays(10),
                        "poster.png",
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(5),
                        MarathonStatus.OPEN
                )
        );
    }

    private Course saveCourse(Marathon marathon, int capacity, int currentCount) {
        Course course = new Course(
                "10K",
                BigDecimal.valueOf(30000),
                capacity,
                currentCount
        );
        marathon.addCourse(course);
        marathonRepository.saveAndFlush(marathon);
        return courseRepository.findAllByMarathon_IdOrderByIdAsc(marathon.getId()).get(0);
    }

    private Registration saveRegistration(Users user, Course course, Marathon marathon) {
        return registrationRepository.saveAndFlush(
                Registration.create(user, course, marathon, "12345", "서울시 강남구", "101동", "L", true)
        );
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId).orElseThrow();
    }

    private boolean containsConstraintName(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
