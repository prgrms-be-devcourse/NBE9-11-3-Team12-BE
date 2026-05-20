package com.rungo.api.domain.registration.queue.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.dto.CreateRegistrationRes
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import com.rungo.api.domain.registration.queue.config.RegistrationQueueProperties
import com.rungo.api.domain.registration.queue.dto.RegistrationQueuePayload
import com.rungo.api.domain.registration.queue.dto.RegistrationQueueResult
import com.rungo.api.domain.registration.queue.repository.RegistrationQueueRepository
import com.rungo.api.domain.registration.service.RegistrationService
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.hibernate.exception.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.sql.SQLException
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class RegistrationQueueServiceTest {

    @Mock
    private lateinit var registrationService: RegistrationService

    private lateinit var redissonClient: RedissonClient
    private lateinit var properties: RegistrationQueueProperties
    private lateinit var registrationQueueRepository: RegistrationQueueRepository
    private lateinit var registrationQueueService: RegistrationQueueService

    @BeforeEach
    fun setUp() {
        redissonClient = Redisson.create(
            Config().apply {
                useSingleServer().address = "redis://localhost:16379"
            }
        )
        properties = RegistrationQueueProperties(
            schedulerEnabled = false,
            pollInterval = 100,
            workerConcurrency = 5,
            courseQueueMaxSize = 300,
            processingTtl = 30,
            payloadTtl = 5,
            resultTtl = 5
        )
        registrationQueueRepository = RegistrationQueueRepository(
            redissonClient = redissonClient,
            properties = properties,
            objectMapper = objectMapper()
        )
        clearRegistrationQueueKeys()
        registrationQueueService = RegistrationQueueService(
            registrationQueueRepository = registrationQueueRepository,
            registrationService = registrationService,
            properties = properties
        )
    }

    @AfterEach
    fun tearDown() {
        clearRegistrationQueueKeys()
        redissonClient.shutdown()
    }

    private fun clearRegistrationQueueKeys() {
        redissonClient.keys.deleteByPattern("queue:registration:*")
    }

    @Test
    @DisplayName("대기열 적재 성공 - payload 저장 후 waiting queue와 active course에 등록한다")
    fun enqueue_success() {
        val request = createRegistrationRequest(courseId = 10L)

        val requestId = registrationQueueService.enqueue(1L, request)

        val savedPayload = registrationQueueRepository.findPayload(requestId)

        assertThat(savedPayload).isNotNull
        assertThat(savedPayload!!.userId).isEqualTo(1L)
        assertThat(savedPayload.courseId).isEqualTo(10L)
        assertThat(savedPayload.snapZipCode).isEqualTo("12345")
        assertThat(savedPayload.snapAddress).isEqualTo("서울시 강남구")
        assertThat(savedPayload.snapDetail).isEqualTo("101동")
        assertThat(savedPayload.tSize).isEqualTo("L")
        assertThat(savedPayload.agreedTerms).isTrue()
        assertThat(registrationQueueRepository.peekWaitingRequestId(10L)).isEqualTo(requestId)
        assertThat(registrationQueueRepository.waitingSize(10L)).isEqualTo(1)
        assertThat(registrationQueueRepository.findActiveCourseIds()).containsExactly(10L)
        assertThat(registrationQueueRepository.existsDedupe(1L, 10L)).isTrue()
    }

    @Test
    @DisplayName("대기열 적재 실패 - 같은 사용자와 같은 코스 요청은 한 번만 적재할 수 있다")
    fun enqueue_fail_duplicate_request() {
        val request = createRegistrationRequest(courseId = 10L)

        registrationQueueService.enqueue(1L, request)

        val exception = assertThrows<CustomException> {
            registrationQueueService.enqueue(1L, request)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.REGISTRATION_ALREADY_EXISTS)
        assertThat(registrationQueueRepository.waitingSize(10L)).isEqualTo(1)
        assertThat(registrationQueueRepository.findActiveCourseIds()).containsExactly(10L)
        assertThat(registrationQueueRepository.existsDedupe(1L, 10L)).isTrue()
    }

    @Test
    @DisplayName("대기열 적재 실패 - enqueue가 실패하면 payload와 dedupe를 롤백한다")
    fun enqueue_fail_and_rollback_when_enqueue_returns_false() {
        val mockedQueueRepository = mock(RegistrationQueueRepository::class.java)
        val localService = RegistrationQueueService(
            registrationQueueRepository = mockedQueueRepository,
            registrationService = registrationService,
            properties = properties
        )
        val request = createRegistrationRequest(courseId = 10L)
        given(
            mockedQueueRepository.trySetDedupe(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(5L)
            )
        ).willReturn(true)
        given(
            mockedQueueRepository.enqueue(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyDouble()
            )
        ).willReturn(false)

        assertThrows<IllegalStateException> {
            localService.enqueue(1L, request)
        }

        verify(mockedQueueRepository).enqueue(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyDouble()
        )
        verify(mockedQueueRepository).removeWaitingRequest(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.anyString()
        )
        verify(mockedQueueRepository).deletePayload(org.mockito.ArgumentMatchers.anyString())
        verify(mockedQueueRepository).deleteDedupe(
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.eq(10L)
        )
        verify(mockedQueueRepository, never()).addActiveCourse(org.mockito.ArgumentMatchers.eq(10L))
    }

    @Test
    @DisplayName("대기열 처리 성공 - 접수 생성 결과를 저장하고 processing, payload, dedupe를 정리한다")
    fun process_success() {
        val requestId = "request-1"
        val payload = RegistrationQueuePayload(
            userId = 1L,
            courseId = 10L,
            snapZipCode = "12345",
            snapAddress = "서울시 강남구",
            snapDetail = "101동",
            tSize = "L",
            agreedTerms = true
        )
        val request = createRegistrationRequest(courseId = 10L)
        val response = CreateRegistrationRes(
            registrationId = 1L,
            marathonId = 20L,
            marathonTitle = "서울 마라톤",
            courseId = 10L,
            courseType = "10K",
            status = RegistrationStatus.COMPLETED,
            paymentStatus = null,
            orderId = null,
            amount = null,
            paymentDueAt = null,
            appliedAt = LocalDateTime.of(2026, 5, 19, 12, 0)
        )

        registrationQueueRepository.savePayload(requestId, payload)
        registrationQueueRepository.trySetDedupe(1L, 10L, requestId, 5)
        registrationQueueRepository.tryMarkProcessing(requestId)
        given(registrationService.create(1L, request)).willReturn(response)

        registrationQueueService.process(requestId)

        val result = registrationQueueRepository.findResult(requestId)

        assertThat(result).isNotNull
        assertThat(result!!.success).isTrue()
        assertThat(result.response).isEqualTo(response)
        assertThat(result.errorCode).isNull()
        assertThat(registrationQueueRepository.findPayload(requestId)).isNull()
        assertThat(registrationQueueRepository.isProcessing(requestId)).isFalse()
        assertThat(registrationQueueRepository.existsDedupe(1L, 10L)).isFalse()
    }

    @Test
    @DisplayName("대기열 처리 실패 - 비즈니스 예외가 발생하면 실패 결과를 저장하고 processing, payload, dedupe를 정리한다")
    fun process_fail_with_custom_exception() {
        val requestId = "request-2"
        val payload = RegistrationQueuePayload(
            userId = 1L,
            courseId = 10L,
            snapZipCode = "12345",
            snapAddress = "서울시 강남구",
            snapDetail = "101동",
            tSize = "L",
            agreedTerms = true
        )
        val request = createRegistrationRequest(courseId = 10L)

        registrationQueueRepository.savePayload(requestId, payload)
        registrationQueueRepository.trySetDedupe(1L, 10L, requestId, 5)
        registrationQueueRepository.tryMarkProcessing(requestId)
        given(registrationService.create(1L, request))
            .willThrow(CustomException(ErrorCode.CAPACITY_FULL))

        registrationQueueService.process(requestId)

        val result = registrationQueueRepository.findResult(requestId)

        assertThat(result).isNotNull
        assertThat(result!!.success).isFalse()
        assertThat(result.response).isNull()
        assertThat(result.errorCode).isEqualTo(ErrorCode.CAPACITY_FULL.name)
        assertThat(registrationQueueRepository.findPayload(requestId)).isNull()
        assertThat(registrationQueueRepository.isProcessing(requestId)).isFalse()
        assertThat(registrationQueueRepository.existsDedupe(1L, 10L)).isFalse()
    }

    @Test
    @DisplayName("대기열 생성 성공 - 처리 결과가 성공이면 최종 접수 응답을 반환한다")
    fun create_success_when_result_is_success() {
        val mockedQueueRepository = mock(RegistrationQueueRepository::class.java)
        val localService = RegistrationQueueService(
            registrationQueueRepository = mockedQueueRepository,
            registrationService = registrationService,
            properties = properties
        )
        val request = createRegistrationRequest(courseId = 10L)
        val response = CreateRegistrationRes(
            registrationId = 1L,
            marathonId = 20L,
            marathonTitle = "서울 마라톤",
            courseId = 10L,
            courseType = "10K",
            status = RegistrationStatus.COMPLETED,
            paymentStatus = null,
            orderId = null,
            amount = null,
            paymentDueAt = null,
            appliedAt = LocalDateTime.of(2026, 5, 19, 12, 0)
        )

        given(
            mockedQueueRepository.trySetDedupe(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(5L)
            )
        ).willReturn(true)
        given(
            mockedQueueRepository.enqueue(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyDouble()
            )
        ).willReturn(true)
        given(mockedQueueRepository.findResult(org.mockito.ArgumentMatchers.anyString()))
            .willReturn(RegistrationQueueResult(success = true, response = response))

        val actual = localService.create(1L, request)

        assertThat(actual).isEqualTo(response)
        verify(mockedQueueRepository).deleteResult(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    @DisplayName("대기열 생성 실패 - 처리 결과가 실패이면 비즈니스 예외로 복원한다")
    fun create_fail_when_result_is_failure() {
        val mockedQueueRepository = mock(RegistrationQueueRepository::class.java)
        val localService = RegistrationQueueService(
            registrationQueueRepository = mockedQueueRepository,
            registrationService = registrationService,
            properties = properties
        )
        val request = createRegistrationRequest(courseId = 10L)

        given(
            mockedQueueRepository.trySetDedupe(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(5L)
            )
        ).willReturn(true)
        given(
            mockedQueueRepository.enqueue(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyDouble()
            )
        ).willReturn(true)
        given(mockedQueueRepository.findResult(org.mockito.ArgumentMatchers.anyString()))
            .willReturn(RegistrationQueueResult(success = false, errorCode = ErrorCode.CAPACITY_FULL.name))

        val exception = assertThrows<CustomException> {
            localService.create(1L, request)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.CAPACITY_FULL)
        verify(mockedQueueRepository).deleteResult(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    @DisplayName("대기열 처리 실패 - 중복 접수 제약 예외가 발생하면 REGISTRATION_ALREADY_EXISTS 결과를 저장한다")
    fun process_fail_with_duplicate_constraint_exception() {
        val requestId = "request-3"
        val payload = RegistrationQueuePayload(
            userId = 1L,
            courseId = 10L,
            snapZipCode = "12345",
            snapAddress = "서울시 강남구",
            snapDetail = "101동",
            tSize = "L",
            agreedTerms = true
        )
        val request = createRegistrationRequest(courseId = 10L)
        val exception = DataIntegrityViolationException(
            "duplicate registration",
            ConstraintViolationException(
                "duplicate registration",
                SQLException("duplicate"),
                "uk_registration_user_marathon"
            )
        )

        registrationQueueRepository.savePayload(requestId, payload)
        registrationQueueRepository.trySetDedupe(1L, 10L, requestId, 5)
        registrationQueueRepository.tryMarkProcessing(requestId)
        given(registrationService.create(1L, request)).willThrow(exception)

        registrationQueueService.process(requestId)

        val result = registrationQueueRepository.findResult(requestId)

        assertThat(result).isNotNull
        assertThat(result!!.success).isFalse()
        assertThat(result.response).isNull()
        assertThat(result.errorCode).isEqualTo(ErrorCode.REGISTRATION_ALREADY_EXISTS.name)
        assertThat(registrationQueueRepository.findPayload(requestId)).isNull()
        assertThat(registrationQueueRepository.isProcessing(requestId)).isFalse()
        assertThat(registrationQueueRepository.existsDedupe(1L, 10L)).isFalse()
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
