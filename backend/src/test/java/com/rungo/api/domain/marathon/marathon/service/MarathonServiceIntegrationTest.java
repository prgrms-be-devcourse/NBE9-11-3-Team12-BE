package com.rungo.api.domain.marathon.marathon.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.course.repository.CourseRepository;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
class MarathonServiceIntegrationTest {

    @Autowired
    private MarathonService marathonService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Autowired
    private MarathonRepository marathonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

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
    @DisplayName("대회 취소 성공 시 참가자 대상 이메일이 비동기로 발송되고 상태가 변경된다")
    void cancel_marathon_success_email_send_test() {
        Users organizer = saveOrganizer("organizer@test.com");
        Users participant1 = saveParticipant("user1@test.com");
        Users participant2 = saveParticipant("user2@test.com");

        Marathon marathon = saveMarathon(organizer);
        Course course = saveCourse(marathon);

        saveRegistration(participant1, marathon, course);
        saveRegistration(participant2, marathon, course);

        marathonService.cancelMarathon(organizer.getId(), marathon.getId());

        Marathon savedMarathon = marathonRepository.findById(marathon.getId()).orElseThrow();
        assertThat(savedMarathon.getStatus()).isEqualTo(MarathonStatus.CANCELED);

        verify(emailService, timeout(2000).times(2))
                .send(any(EmailMessage.class));

    }

    @Test
    @DisplayName("대회 취소 중 이메일 발송 실패가 발생해도 상태 변경은 정상 커밋된다")
    void cancel_marathon_email_exception_isolation_test() {
        doThrow(new RuntimeException("SMTP 서버 강제 다운"))
                .when(emailService).send(any(EmailMessage.class));


        Users organizer = saveOrganizer("organizer-fail@test.com");
        Users participant1 = saveParticipant("fail-user1@test.com");
        Users participant2 = saveParticipant("fail-user2@test.com");

        Marathon marathon = saveMarathon(organizer);
        Course course = saveCourse(marathon);

        saveRegistration(participant1, marathon, course);
        saveRegistration(participant2, marathon, course);

        marathonService.cancelMarathon(organizer.getId(), marathon.getId());

        Marathon savedMarathon = marathonRepository.findById(marathon.getId()).orElseThrow();
        assertThat(savedMarathon.getStatus()).isEqualTo(MarathonStatus.CANCELED);

        verify(emailService, timeout(2000).atLeastOnce())
                .send(any(EmailMessage.class));
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

    private Marathon saveMarathon(Users organizer) {
        return marathonRepository.save(
                new Marathon(
                        organizer,
                        "서울 마라톤",
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

    private Course saveCourse(Marathon marathon) {
        Course course = new Course(
                "10K",
                BigDecimal.valueOf(30000),
                100,
                2
        );
        marathon.addCourse(course);
        Marathon savedMarathon = marathonRepository.save(marathon);
        return savedMarathon.getCourses().get(0);
    }

    private Registration saveRegistration(Users user, Marathon marathon, Course course) {
        return registrationRepository.save(
                Registration.create(
                        user,
                        course,
                        marathon,
                        "12345",            // snapZipCode
                        "서울시 강남구",      // snapAddress
                        "101동",            // snapDetail
                        "L",                // tSize
                        true                // agreedTerms
                )
        );
    }
}
