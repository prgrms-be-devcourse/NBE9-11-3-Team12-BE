package com.rungo.api.domain.registration.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.course.repository.CourseRepository;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.registration.dto.CreateRegistrationReq;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.domain.auth.repository.UserAuthRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import com.rungo.api.global.infrastructure.mail.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RegistrationCommandServiceConcurrencyTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private RegistrationCancelHistoryRepository registrationCancelHistoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MarathonRepository marathonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    @AfterEach
    void clearData() {
        registrationCancelHistoryRepository.deleteAllInBatch();
        registrationRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        marathonRepository.deleteAllInBatch();
        userAuthRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("정원이 1명인 코스에 동시에 신청하면 1건만 성공한다")
    void create_concurrently_only_one_succeeds() throws Exception {
        Marathon marathon = saveMarathon("서울 마라톤");
        Course course = saveCourse(marathon, 1, 0);
        List<Users> participants = saveParticipants(10);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger capacityFullCount = new AtomicInteger();
        List<Throwable> unexpectedErrors = new ArrayList<>();

        // 여러 요청이 최대한 같은 시점에 create()를 호출하도록 맞춘다.
        runConcurrently(participants.size(), index -> {
            try {
                registrationService.create(
                        participants.get(index).getId(),
                        createRequest(course.getId())
                );
                successCount.incrementAndGet();
            } catch (CustomException exception) {
                if (exception.getErrorCode() == ErrorCode.CAPACITY_FULL) {
                    capacityFullCount.incrementAndGet();
                    return;
                }
                synchronized (unexpectedErrors) {
                    unexpectedErrors.add(exception);
                }
            } catch (Throwable throwable) {
                synchronized (unexpectedErrors) {
                    unexpectedErrors.add(throwable);
                }
            }
        });

        // 최종 정합성은 메모리 값이 아니라 DB에 반영된 상태로 확인한다.
        System.out.println("동시 신청 결과"
                + " | 요청 수=" + participants.size()
                + " | 성공=" + successCount.get()
                + " | 정원 마감 실패=" + capacityFullCount.get()
                + " | currentCount=" + findCourse(course.getId()).getCurrentCount()
                + " | registrationCount=" + registrationRepository.count());

        assertTrue(unexpectedErrors.isEmpty());
        assertEquals(1, successCount.get());
        assertEquals(participants.size() - 1, capacityFullCount.get());
        assertEquals(1, findCourse(course.getId()).getCurrentCount());
        assertEquals(1, registrationRepository.count());
    }

    @Test
    @DisplayName("정원이 100명인 코스에 100명이 동시에 신청하면 모두 성공하고 count가 100이 된다")
    void create_concurrently_one_hundred_requests_fill_capacity_exactly() throws Exception {
        // 동시 요청이 몰려도 currentCount와 실제 접수 수가 정확히 누적되는지 확인한다.
        int requestCount = 100;
        Marathon marathon = saveMarathon("제주 마라톤");
        Course course = saveCourse(marathon, requestCount, 0);
        List<Users> participants = saveParticipants(requestCount);

        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> unexpectedErrors = new ArrayList<>();

        runConcurrently(participants.size(), index -> {
            try {
                registrationService.create(
                        participants.get(index).getId(),
                        createRequest(course.getId())
                );
                successCount.incrementAndGet();
            } catch (Throwable throwable) {
                synchronized (unexpectedErrors) {
                    unexpectedErrors.add(throwable);
                }
            }
        });

        long registrationCount = registrationRepository.count();
        int currentCount = findCourse(course.getId()).getCurrentCount();

        System.out.println("100명 동시 신청 결과"
                + " | 요청 수=" + participants.size()
                + " | 성공=" + successCount.get()
                + " | 실패=" + unexpectedErrors.size()
                + " | currentCount=" + currentCount
                + " | registrationCount=" + registrationCount);

        assertTrue(unexpectedErrors.isEmpty());
        assertEquals(requestCount, successCount.get());
        assertEquals(requestCount, currentCount);
        assertEquals(requestCount, registrationCount);
    }

    @Test
    @DisplayName("동일 접수를 동시에 취소하면 1건만 성공하고 취소 이력은 1건만 저장된다")
    void cancel_concurrently_only_one_succeeds() throws Exception {
        Users participant = saveUser("cancel-participant@test.com", Role.PARTICIPANT);
        Marathon marathon = saveMarathon("부산 마라톤");
        Course course = saveCourse(marathon, 10, 1);
        Registration registration = saveRegistration(participant, course, marathon);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger expectedFailureCount = new AtomicInteger();
        List<Throwable> unexpectedErrors = new ArrayList<>();

        runConcurrently(2, index -> {
            try {
                registrationService.cancel(participant.getId(), registration.getId());
                successCount.incrementAndGet();
            } catch (CustomException exception) {
                if (exception.getErrorCode() == ErrorCode.REGISTRATION_NOT_FOUND) {
                    expectedFailureCount.incrementAndGet();
                    return;
                }
                synchronized (unexpectedErrors) {
                    unexpectedErrors.add(exception);
                }
            } catch (DataIntegrityViolationException exception) {
                expectedFailureCount.incrementAndGet();
            } catch (Throwable throwable) {
                synchronized (unexpectedErrors) {
                    unexpectedErrors.add(throwable);
                }
            }
        });

        assertTrue(unexpectedErrors.isEmpty());
        assertEquals(1, successCount.get());
        assertEquals(1, expectedFailureCount.get());
        assertEquals(0, registrationRepository.count());
        assertEquals(1, registrationCancelHistoryRepository.count());
        assertEquals(0, findCourse(course.getId()).getCurrentCount());
    }

    private void runConcurrently(int threadCount, ConcurrentAction action) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                futures.add(executorService.submit(() -> {
                    readyLatch.countDown();
                    // 모든 스레드가 준비될 때까지 대기했다가 한 번에 출발한다.
                    startLatch.await();
                    action.run(index);
                    return null;
                }));
            }

            readyLatch.await();
            startLatch.countDown();

            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private List<Users> saveParticipants(int count) {
        List<Users> participants = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            participants.add(saveUser("participant" + i + "@test.com", Role.PARTICIPANT));
        }
        return participants;
    }

    private Users saveUser(String email, Role role) {
        return userRepository.saveAndFlush(
                Users.builder()
                        .email(email)
                        .name(email)
                        .phoneNumber("010-1111-2222")
                        .role(role)
                        .gender(Gender.MALE)
                        .birth(LocalDate.of(2000, 1, 1))
                        .build()
        );
    }

    private Marathon saveMarathon(String title) {
        Users organizer = saveUser(title + "@organizer.com", Role.ORGANIZER);
        return marathonRepository.saveAndFlush(
                new Marathon(
                        organizer,
                        title,
                        "서울",
                        "성동구",
                        LocalDate.of(2026, 10, 3),
                        "poster.png",
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(1),
                        MarathonStatus.OPEN
                )
        );
    }

    private Course saveCourse(Marathon marathon, int capacity, int currentCount) {
        Course course = new Course("10K", BigDecimal.valueOf(30000), capacity, currentCount);
        marathon.addCourse(course);
        marathonRepository.saveAndFlush(marathon);
        return courseRepository.findAllByMarathon_IdOrderByIdAsc(marathon.getId()).get(0);
    }

    private CreateRegistrationReq createRequest(Long courseId) {
        return new CreateRegistrationReq(courseId, "12345", "서울시 강남구", "101동", "L", true);
    }

    private Registration saveRegistration(Users user, Course course, Marathon marathon) {
        return registrationRepository.saveAndFlush(
                Registration.create(user, course, marathon, "12345", "서울시 강남구", "101동", "L", true)
        );
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId).orElseThrow();
    }

    @FunctionalInterface
    private interface ConcurrentAction {
        void run(int index) throws Exception;
    }
}
