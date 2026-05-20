package com.rungo.api.domain.registration.queue.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.queue.config.RegistrationQueueProperties
import com.rungo.api.domain.registration.queue.repository.RegistrationQueueRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RegistrationQueueServiceTest {

    @Mock
    private lateinit var courseRepository: CourseRepository

    private lateinit var redissonClient: RedissonClient
    private lateinit var registrationQueueRepository: RegistrationQueueRepository
    private lateinit var registrationQueueService: RegistrationQueueService

    @BeforeEach
    fun setUp() {
        redissonClient = Redisson.create(
            Config().apply {
                useSingleServer().address = "redis://localhost:16379"
            }
        )
        registrationQueueRepository = RegistrationQueueRepository(
            redissonClient = redissonClient,
            properties = RegistrationQueueProperties(
                schedulerEnabled = false,
                pollInterval = 100,
                workerConcurrency = 5,
                courseQueueMaxSize = 300,
                processingTtl = 30,
                payloadTtl = 5,
                resultTtl = 5
            ),
            objectMapper = objectMapper()
        )
        registrationQueueService = RegistrationQueueService(
            registrationQueueRepository = registrationQueueRepository,
            courseRepository = courseRepository
        )
    }

    @AfterEach
    fun tearDown() {
        redissonClient.keys.deleteByPattern("queue:registration:*")
        redissonClient.shutdown()
    }

    @Test
    @DisplayName("대기열 적재 성공 - payload 저장 후 waiting queue와 active course에 등록한다")
    fun enqueue_success() {
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(1),
            status = MarathonStatus.OPEN
        ).also { setField(it, "id", 20L) }
        val course = createCourse(marathon = marathon, capacity = 100, currentCount = 10)
            .also { setField(it, "id", 10L) }
        val request = createRegistrationRequest(courseId = 10L)

        given(courseRepository.findById(10L)).willReturn(Optional.of(course))

        val requestId = registrationQueueService.enqueue(1L, request)

        val savedPayload = registrationQueueRepository.findPayload(requestId)

        assertThat(savedPayload).isNotNull
        assertThat(savedPayload!!.userId).isEqualTo(1L)
        assertThat(savedPayload.marathonId).isEqualTo(20L)
        assertThat(savedPayload.courseId).isEqualTo(10L)
        assertThat(savedPayload.snapZipCode).isEqualTo("12345")
        assertThat(savedPayload.snapAddress).isEqualTo("서울시 강남구")
        assertThat(savedPayload.snapDetail).isEqualTo("101동")
        assertThat(savedPayload.tSize).isEqualTo("L")
        assertThat(savedPayload.agreedTerms).isTrue()
        assertThat(registrationQueueRepository.peekWaitingRequestId(10L)).isEqualTo(requestId)
        assertThat(registrationQueueRepository.waitingSize(10L)).isEqualTo(1)
        assertThat(registrationQueueRepository.findActiveCourseIds()).containsExactly(10L)
    }

    @Test
    @DisplayName("대기열 적재 실패 - 코스가 없으면 COURSE_NOT_FOUND 예외가 발생하고 queue 데이터가 남지 않는다")
    fun enqueue_fail_course_not_found() {
        val request = createRegistrationRequest(courseId = 99L)
        given(courseRepository.findById(99L)).willReturn(Optional.empty())

        val exception = assertThrows<CustomException> {
            registrationQueueService.enqueue(1L, request)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.COURSE_NOT_FOUND)
        assertThat(registrationQueueRepository.findActiveCourseIds()).isEmpty()
        assertThat(registrationQueueRepository.waitingSize(99L)).isZero()
    }

    private fun objectMapper(): ObjectMapper =
        ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())

    private fun createRegistrationRequest(
        courseId: Long = 1L,
        agreedTerms: Boolean = true
    ): CreateRegistrationReq = CreateRegistrationReq(
        courseId = courseId,
        snapZipCode = "12345",
        snapAddress = "서울시 강남구",
        snapDetail = "101동",
        tSize = "L",
        agreedTerms = agreedTerms
    )

    private fun createUser(id: Long, name: String, phoneNumber: String): Users =
        Users.create(
            email = "test@test.com",
            name = name,
            phoneNumber = phoneNumber,
            gender = Gender.MALE,
            birth = LocalDate.of(2000, 1, 1)
        ).also { setField(it, "id", id) }

    private fun createMarathon(
        registrationStartAt: LocalDateTime,
        registrationEndAt: LocalDateTime,
        status: MarathonStatus,
    ): Marathon =
        Marathon.create(
            organizer = createUser(id = 99L, name = "주최자", phoneNumber = "010-9999-9999"),
            title = "서울 마라톤",
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.of(2026, 10, 3),
            posterImageUrl = "poster.png",
            registrationStartAt = registrationStartAt,
            registrationEndAt = registrationEndAt
        ).also { setField(it, "status", status) }

    private fun createCourse(
        marathon: Marathon,
        capacity: Int,
        currentCount: Int,
    ): Course =
        Course.create(
            courseType = "10K",
            price = BigDecimal.valueOf(30000),
            capacity = capacity,
            currentCount = currentCount
        ).also { marathon.addCourse(it) }

    private fun setField(target: Any, name: String, value: Any?) {
        ReflectionTestUtils.setField(target, name, value)
    }
}
