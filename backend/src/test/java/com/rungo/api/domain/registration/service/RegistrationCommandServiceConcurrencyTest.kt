package com.rungo.api.domain.registration.service

import com.rungo.api.domain.auth.repository.UserAuthRepository
import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.infrastructure.mail.EmailService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class RegistrationCommandServiceConcurrencyTest {

    @Autowired
    private lateinit var registrationService: RegistrationService

    @Autowired
    private lateinit var registrationRepository: RegistrationRepository

    @Autowired
    private lateinit var registrationCancelHistoryRepository: RegistrationCancelHistoryRepository

    @Autowired
    private lateinit var courseRepository: CourseRepository

    @Autowired
    private lateinit var marathonRepository: MarathonRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userAuthRepository: UserAuthRepository

    @MockitoBean
    private lateinit var emailService: EmailService

    @BeforeEach
    @AfterEach
    fun clearData() {
        registrationCancelHistoryRepository.deleteAllInBatch()
        registrationRepository.deleteAllInBatch()
        courseRepository.deleteAllInBatch()
        marathonRepository.deleteAllInBatch()
        userAuthRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("정원이 1명인 코스에 동시에 신청하면 1건만 성공한다")
    fun create_concurrently_only_one_succeeds() {
        val marathon = saveMarathon("서울 마라톤")
        val course = saveCourse(marathon = marathon, capacity = 1, currentCount = 0)
        val participants = saveParticipants(10)

        val successCount = AtomicInteger()
        val capacityFullCount = AtomicInteger()
        val unexpectedErrors = mutableListOf<Throwable>()

        runConcurrently(participants.size) { index ->
            try {
                registrationService.create(
                    participants[index].id,
                    createRegistrationRequest(course.id)
                )
                successCount.incrementAndGet()
            } catch (exception: CustomException) {
                if (exception.errorCode == ErrorCode.CAPACITY_FULL) {
                    capacityFullCount.incrementAndGet()
                    return@runConcurrently
                }
                synchronized(unexpectedErrors) {
                    unexpectedErrors.add(exception)
                }
            } catch (throwable: Throwable) {
                synchronized(unexpectedErrors) {
                    unexpectedErrors.add(throwable)
                }
            }
        }

        println(
            "동시 신청 결과" +
                " | 요청 수=${participants.size}" +
                " | 성공=${successCount.get()}" +
                " | 정원 마감 실패=${capacityFullCount.get()}" +
                " | currentCount=${findCourse(course.id).currentCount}" +
                " | registrationCount=${registrationRepository.count()}"
        )

        assertTrue(unexpectedErrors.isEmpty())
        assertEquals(1, successCount.get())
        assertEquals(participants.size - 1, capacityFullCount.get())
        assertEquals(1, findCourse(course.id).currentCount)
        assertEquals(1, registrationRepository.count())
    }

    @Test
    @DisplayName("정원이 100명인 코스에 100명이 동시에 신청하면 모두 성공하고 count가 100이 된다")
    fun create_concurrently_one_hundred_requests_fill_capacity_exactly() {
        val requestCount = 100
        val marathon = saveMarathon("제주 마라톤")
        val course = saveCourse(marathon = marathon, capacity = requestCount, currentCount = 0)
        val participants = saveParticipants(requestCount)

        val successCount = AtomicInteger()
        val unexpectedErrors = mutableListOf<Throwable>()

        runConcurrently(participants.size) { index ->
            try {
                registrationService.create(
                    participants[index].id,
                    createRegistrationRequest(course.id)
                )
                successCount.incrementAndGet()
            } catch (throwable: Throwable) {
                synchronized(unexpectedErrors) {
                    unexpectedErrors.add(throwable)
                }
            }
        }

        val registrationCount = registrationRepository.count()
        val currentCount = findCourse(course.id).currentCount

        println(
            "100명 동시 신청 결과" +
                " | 요청 수=${participants.size}" +
                " | 성공=${successCount.get()}" +
                " | 실패=${unexpectedErrors.size}" +
                " | currentCount=$currentCount" +
                " | registrationCount=$registrationCount"
        )

        assertTrue(unexpectedErrors.isEmpty())
        assertEquals(requestCount, successCount.get())
        assertEquals(requestCount, currentCount)
        assertEquals(requestCount.toLong(), registrationCount)
    }

    @Test
    @DisplayName("동일 접수를 동시에 취소하면 1건만 성공하고 취소 이력은 1건만 저장된다")
    fun cancel_concurrently_only_one_succeeds() {
        val participant = saveUser(email = "cancel-participant@test.com", role = Role.PARTICIPANT)
        val marathon = saveMarathon("부산 마라톤")
        val course = saveCourse(marathon = marathon, capacity = 10, currentCount = 1)
        val registration = saveRegistration(participant, course, marathon)

        val successCount = AtomicInteger()
        val expectedFailureCount = AtomicInteger()
        val unexpectedErrors = mutableListOf<Throwable>()

        runConcurrently(2) {
            try {
                registrationService.cancel(participant.id, registration.id)
                successCount.incrementAndGet()
            } catch (exception: CustomException) {
                if (exception.errorCode == ErrorCode.REGISTRATION_NOT_FOUND) {
                    expectedFailureCount.incrementAndGet()
                    return@runConcurrently
                }
                synchronized(unexpectedErrors) {
                    unexpectedErrors.add(exception)
                }
            } catch (exception: DataIntegrityViolationException) {
                expectedFailureCount.incrementAndGet()
            } catch (throwable: Throwable) {
                synchronized(unexpectedErrors) {
                    unexpectedErrors.add(throwable)
                }
            }
        }

        assertTrue(unexpectedErrors.isEmpty())
        assertEquals(1, successCount.get())
        assertEquals(1, expectedFailureCount.get())
        assertEquals(0, registrationRepository.count())
        assertEquals(1, registrationCancelHistoryRepository.count())
        assertEquals(0, findCourse(course.id).currentCount)
    }

    private fun runConcurrently(threadCount: Int, action: (Int) -> Unit) {
        val executorService = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val futures = mutableListOf<Future<*>>()

        try {
            repeat(threadCount) { index ->
                futures += executorService.submit<Unit> {
                    readyLatch.countDown()
                    startLatch.await()
                    action(index)
                }
            }

            readyLatch.await()
            startLatch.countDown()
            futures.forEach(Future<*>::get)
        } finally {
            executorService.shutdownNow()
        }
    }

    private fun saveParticipants(count: Int): List<Users> =
        List(count) { index ->
            saveUser(
                email = "participant$index@test.com",
                role = Role.PARTICIPANT
            )
        }

    private fun saveUser(email: String, role: Role): Users =
        Users.create(
            email = email,
            name = email,
            phoneNumber = "010-1111-2222",
            gender = Gender.MALE,
            birth = LocalDate.of(2000, 1, 1)
        ).apply {
            if (role == Role.ORGANIZER) {
                promoteToOrganizer()
            }
        }.let(userRepository::saveAndFlush)

    private fun saveMarathon(title: String): Marathon {
        val organizer = saveUser(email = "$title@organizer.com", role = Role.ORGANIZER)

        return Marathon.create(
            organizer = organizer,
            title = title,
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.of(2026, 10, 3),
            posterImageUrl = "poster.png",
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(1)
        ).let(marathonRepository::saveAndFlush)
    }

    private fun saveCourse(marathon: Marathon, capacity: Int, currentCount: Int): Course {
        val course = Course.create(
            courseType = "10K",
            price = BigDecimal.valueOf(30000),
            capacity = capacity,
            currentCount = currentCount
        )

        marathon.addCourse(course)
        marathonRepository.saveAndFlush(marathon)

        return courseRepository.findAllByMarathon_IdOrderByIdAsc(marathon.id).first()
    }

    private fun createRegistrationRequest(courseId: Long): CreateRegistrationReq =
        CreateRegistrationReq(
            courseId = courseId,
            snapZipCode = "12345",
            snapAddress = "서울시 강남구",
            snapDetail = "101동",
            tSize = "L",
            agreedTerms = true
        )

    private fun saveRegistration(user: Users, course: Course, marathon: Marathon): Registration =
        Registration.createCompleted(
            user = user,
            course = course,
            marathon = marathon,
            snapZipCode = "12345",
            snapAddress = "서울시 강남구",
            snapDetail = "101동",
            tSize = "L",
            agreedTerms = true
        ).let(registrationRepository::saveAndFlush)

    private fun findCourse(courseId: Long): Course =
        courseRepository.findByIdOrNull(courseId)
            ?: error("Course not found: $courseId")
}
