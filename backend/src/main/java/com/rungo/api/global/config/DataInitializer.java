package com.rungo.api.global.config;

import com.rungo.api.domain.auth.entity.UserAuth;
import com.rungo.api.domain.auth.repository.UserAuthRepository;
import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.course.repository.CourseRepository;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Profile("!test")
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private static final String TEST_PASSWORD = "Password123!";
    private static final int TEST_USER_COUNT = 100;
    private static final int CANCEL_TEST_MARATHON_COUNT = 100;
    private static final int USER_BATCH_SIZE = 100;

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final MarathonRepository marathonRepository;
    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initTestData() {
        return args -> {
            Users organizer = initOrganizer();
            initParticipants();
            initPerformanceMarathon(organizer);
            initCancelTestMarathons(organizer);

            System.out.println("테스트 유저 / 마라톤 / 코스 / 취소 테스트 데이터 생성 완료");
            System.out.println("organizer: organizer@test.com / " + TEST_PASSWORD);
            System.out.println("participants: user1@test.com ~ user" + TEST_USER_COUNT + "@test.com / " + TEST_PASSWORD);
        };
    }

    private Users initOrganizer() {
        return userRepository.findByEmail("organizer@test.com")
                             .orElseGet(() -> {
                                 Users savedUser = userRepository.save(
                                         Users.builder()
                                              .email("organizer@test.com")
                                              .name("주최자")
                                              .phoneNumber("010-2222-2222")
                                              .role(Role.ORGANIZER)
                                              .gender(Gender.MALE)
                                              .birth(LocalDate.of(2000, 1, 1))
                                              .build()
                                 );

                                 userAuthRepository.save(
                                         UserAuth.createLocalAuth(savedUser, passwordEncoder.encode(TEST_PASSWORD))
                                 );

                                 return savedUser;
                             });
    }

    private void initParticipants() {
        Set<String> existingEmails = new HashSet<>(
                userRepository.findAllByEmailStartingWith("user")
                              .stream()
                              .map(Users::getEmail)
                              .toList()
        );

        List<Users> batch = new ArrayList<>();

        for (int i = 1; i <= TEST_USER_COUNT; i++) {
            String email = "user" + i + "@test.com";

            if (existingEmails.contains(email)) {
                continue;
            }

            batch.add(
                    Users.builder()
                         .email(email)
                         .name("참가자" + i)
                         .phoneNumber(String.format("010-%04d-%04d", i / 10000, i % 10000))
                         .role(Role.PARTICIPANT)
                         .gender(Gender.MALE)
                         .birth(LocalDate.of(2000, 1, 1))
                         .build()
            );

            if (batch.size() == USER_BATCH_SIZE) {
                saveUsersWithLocalAuth(batch);
                System.out.println("테스트 유저 저장 진행중: " + i + "명");
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            saveUsersWithLocalAuth(batch);
        }

        System.out.println("테스트 유저 저장 완료");
    }

    private void saveUsersWithLocalAuth(List<Users> usersBatch) {
        List<Users> savedUsers = userRepository.saveAll(usersBatch);

        List<UserAuth> authBatch = savedUsers.stream()
                                             .map(user -> UserAuth.createLocalAuth(user, passwordEncoder.encode(TEST_PASSWORD)))
                                             .toList();

        userAuthRepository.saveAll(authBatch);
    }

    private void initPerformanceMarathon(Users organizer) {
        boolean exists = marathonRepository.findAllByTitleStartingWith("테스트용 마라톤")
                                           .stream()
                                           .anyMatch(m -> "테스트용 마라톤".equals(m.getTitle()));

        if (exists) {
            return;
        }

        Marathon marathon = new Marathon(
                organizer,
                "테스트용 마라톤",
                "서울",
                "성동구",
                LocalDate.now().plusDays(30),
                "poster.png",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10),
                MarathonStatus.OPEN
        );

        Marathon savedMarathon = marathonRepository.save(marathon);

        Course course = new Course(
                "10K",
                BigDecimal.valueOf(30000),
                40000,
                0
        );

        savedMarathon.addCourse(course);
        courseRepository.save(course);
    }

    private void initCancelTestMarathons(Users organizer) {
        Set<String> existingTitles = new HashSet<>(
                marathonRepository.findAllByTitleStartingWith("취소테스트 마라톤")
                                  .stream()
                                  .map(Marathon::getTitle)
                                  .toList()
        );

        List<Marathon> marathonsToSave = new ArrayList<>();

        for (int i = 1; i <= CANCEL_TEST_MARATHON_COUNT; i++) {
            String marathonTitle = "취소테스트 마라톤 " + i;

            if (existingTitles.contains(marathonTitle)) {
                continue;
            }

            marathonsToSave.add(new Marathon(
                    organizer,
                    marathonTitle,
                    "서울",
                    "성동구",
                    LocalDate.now().plusDays(30),
                    "poster.png",
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(10),
                    MarathonStatus.OPEN
            ));
        }

        if (marathonsToSave.isEmpty()) {
            return;
        }

        List<Marathon> savedMarathons = marathonRepository.saveAll(marathonsToSave);

        List<Course> courses = new ArrayList<>(savedMarathons.size());
        for (Marathon marathon : savedMarathons) {
            Course course = new Course(
                    "10K",
                    BigDecimal.valueOf(30000),
                    40000,
                    0
            );
            marathon.addCourse(course);
            courses.add(course);
        }
        courseRepository.saveAll(courses);

        List<Registration> registrations = new ArrayList<>(savedMarathons.size());
        for (int i = 0; i < savedMarathons.size(); i++) {
            int participantIndex = i + 1;

            Users participant = userRepository.findByEmail("user" + participantIndex + "@test.com")
                                              .orElseThrow(() -> new IllegalStateException(
                                                      "취소 테스트용 참가자 없음: user" + participantIndex + "@test.com"));

            Marathon savedMarathon = savedMarathons.get(i);
            Course savedCourse = savedMarathon.getCourses().get(0);

            registrations.add(
                    Registration.create(
                            participant,
                            savedCourse,
                            savedMarathon,
                            "12345",
                            "서울시 강남구",
                            "101동",
                            "L",
                            true
                    )
            );
        }

        registrationRepository.saveAll(registrations);
    }
}