package com.rungo.api.domain.registration.service

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.domain.payment.entity.Payment
import com.rungo.api.domain.payment.enumtype.PaymentCancelResult
import com.rungo.api.domain.payment.enumtype.PaymentStatus
import com.rungo.api.domain.payment.repository.PaymentRepository
import com.rungo.api.domain.payment.service.PaymentService
import com.rungo.api.domain.payment.support.OrderIdGenerator
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class RegistrationCommandServiceTest {

    private lateinit var registrationService: RegistrationService

    @Mock
    private lateinit var registrationRepository: RegistrationRepository

    @Mock
    private lateinit var registrationCancelHistoryRepository: RegistrationCancelHistoryRepository

    @Mock
    private lateinit var courseRepository: CourseRepository

    @Mock
    private lateinit var marathonRepository: MarathonRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var paymentService: PaymentService

    @Mock
    private lateinit var orderIdGenerator: OrderIdGenerator

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher


    @BeforeEach
    fun setUp() {
        registrationService = RegistrationService(
            registrationRepository = registrationRepository,
            registrationCancelHistoryRepository = registrationCancelHistoryRepository,
            courseRepository = courseRepository,
            marathonRepository = marathonRepository,
            userRepository = userRepository,
            paymentRepository = paymentRepository,
            paymentService = paymentService,
            orderIdGenerator = orderIdGenerator,
            paymentExpireMinutes = 30L,
            eventPublisher = eventPublisher,
        )
    }

    @Test
    @DisplayName("접수 생성 성공 - 저장과 응답 반환 및 코스 인원 증가가 정상 동작한다")
    fun create_success() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(1),
            status = MarathonStatus.OPEN
        ).also { setField(it, "id", 2L) }
        val course = createCourse(marathon = marathon, capacity = 100, currentCount = 10)
            .also { setField(it, "id", 3L) }

        val request = createRegistrationRequest(courseId = 3L)
        val savedRegistration = createRegistration(
            user = user,
            course = course,
            marathon = marathon,
            request = request
        ).also {
            setField(it, "id", 4L)
            setField(it, "appliedAt", LocalDateTime.of(2026, 4, 15, 10, 0))
        }

        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(courseRepository.findById(3L)).willReturn(Optional.of(course))
        given(marathonRepository.findByIdForUpdate(2L)).willReturn(marathon)
        given(courseRepository.increaseCurrentCountIfNotFull(3L)).willReturn(1)
        given(registrationRepository.save(any(Registration::class.java))).willReturn(savedRegistration)

        val result = registrationService.create(1L, request)

        val registrationCaptor = ArgumentCaptor.forClass(Registration::class.java)
        verify(registrationRepository, times(1)).save(registrationCaptor.capture())

        val capturedRegistration = registrationCaptor.value
        assertSame(user, capturedRegistration.user)
        assertSame(course, capturedRegistration.course)
        assertSame(marathon, capturedRegistration.marathon)
        assertEquals("COMPLETED", capturedRegistration.status.name)
        assertEquals("홍길동", capturedRegistration.snapName)
        assertEquals("010-1111-2222", capturedRegistration.snapPhoneNumber)
        assertEquals("12345", capturedRegistration.snapZipCode)
        assertEquals("서울시 강남구", capturedRegistration.snapAddress)
        assertEquals("101동", capturedRegistration.snapDetail)
        assertEquals("L", capturedRegistration.tSize)
        assertEquals(true, capturedRegistration.isAgreedTerms)

        assertNotNull(result)
        assertEquals(4L, result.registrationId)
        assertEquals(2L, result.marathonId)
        assertEquals("서울 마라톤", result.marathonTitle)
        assertEquals(3L, result.courseId)
        assertEquals("10K", result.courseType)
        assertEquals(RegistrationStatus.COMPLETED, result.status)
        assertEquals(LocalDateTime.of(2026, 4, 15, 10, 0), result.appliedAt)

        verify(courseRepository, times(1)).increaseCurrentCountIfNotFull(3L)
        verify(eventPublisher, times(1)).publishEvent(any(RegistrationCompletedEvent::class.java))
    }

    @Test
    @DisplayName("유료 접수 생성 성공 - 결제 대기 Payment를 생성하고 접수 완료 이벤트는 발행하지 않는다")
    fun create_paid_course_success_creates_ready_payment_without_event() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(1),
            status = MarathonStatus.OPEN
        ).also { setField(it, "id", 2L) }
        val course = createCourse(
            marathon = marathon,
            capacity = 100,
            currentCount = 10,
            price = BigDecimal.valueOf(30000)
        ).also { setField(it, "id", 3L) }

        val request = createRegistrationRequest(courseId = 3L)

        val savedRegistration =
            Registration.createPendingPayment(
                user = user,
                course = course,
                marathon = marathon,
                snapZipCode = request.snapZipCode,
                snapAddress = request.snapAddress,
                snapDetail = request.snapDetail,
                tSize = request.tSize,
                agreedTerms = request.agreedTerms,
            ).also {
                setField(it, "id", 4L)
                setField(it, "appliedAt", LocalDateTime.of(2026, 4, 15, 10, 0))
            }

        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(courseRepository.findById(3L)).willReturn(Optional.of(course))
        given(marathonRepository.findByIdForUpdate(2L)).willReturn(marathon)
        given(courseRepository.increaseCurrentCountIfNotFull(3L)).willReturn(1)
        given(registrationRepository.save(any(Registration::class.java))).willReturn(savedRegistration)
        given(orderIdGenerator.generate(anyLong(), any(LocalDateTime::class.java))).willReturn("ORDER-1")
        given(paymentRepository.save(any(Payment::class.java))).willAnswer { invocation ->
            invocation.arguments[0] as Payment
        }

        val result = registrationService.create(1L, request)

        assertEquals(4L, result.registrationId)
        assertEquals(RegistrationStatus.PENDING_PAYMENT, result.status)
        assertEquals(PaymentStatus.READY, result.paymentStatus)
        assertEquals("ORDER-1", result.orderId)
        assertEquals(30000L, result.amount)

        verify(registrationRepository, times(1)).save(any(Registration::class.java))
        verify(paymentRepository, times(1)).save(any(Payment::class.java))
        verify(eventPublisher, times(0)).publishEvent(any(RegistrationCompletedEvent::class.java))
    }

    @Test
    @DisplayName("접수 생성 실패 - 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    fun create_fail_user_not_found() {
        val request = createRegistrationRequest()
        given(userRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows<CustomException> {
            registrationService.create(1L, request)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("접수 생성 실패 - 약관에 동의하지 않으면 REGISTRATION_TERMS_REQUIRED 예외가 발생한다")
    fun create_fail_terms_required() {
        val request = createRegistrationRequest(agreedTerms = false)

        val exception = assertThrows<CustomException> {
            registrationService.create(1L, request)
        }

        assertEquals(ErrorCode.REGISTRATION_TERMS_REQUIRED, exception.errorCode)
    }

    @Test
    @DisplayName("접수 생성 실패 - 코스가 없으면 COURSE_NOT_FOUND 예외가 발생한다")
    fun create_fail_course_not_found() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val request = createRegistrationRequest()

        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(courseRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows<CustomException> {
            registrationService.create(1L, request)
        }

        assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("접수 생성 실패 - 접수 시작 전이면 REGISTRATION_PERIOD_INVALID 예외가 발생한다")
    fun create_fail_before_registration_start() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().plusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(2),
            status = MarathonStatus.OPEN
        )
        val course = createCourse(marathon = marathon, capacity = 100, currentCount = 10)
        val request = createRegistrationRequest()

        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(courseRepository.findById(1L)).willReturn(Optional.of(course))
        given(marathonRepository.findByIdForUpdate(2L)).willReturn(marathon)

        val exception = assertThrows<CustomException> {
            registrationService.create(1L, request)
        }

        assertEquals(ErrorCode.REGISTRATION_PERIOD_INVALID, exception.errorCode)
    }

    @Test
    @DisplayName("접수 생성 실패 - 접수 종료 후이면 REGISTRATION_PERIOD_INVALID 예외가 발생한다")
    fun create_fail_after_registration_end() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(2),
            registrationEndAt = LocalDateTime.now().minusDays(1),
            status = MarathonStatus.OPEN
        )
        val course = createCourse(marathon = marathon, capacity = 100, currentCount = 10)
        val request = createRegistrationRequest()

        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(courseRepository.findById(1L)).willReturn(Optional.of(course))
        given(marathonRepository.findByIdForUpdate(2L)).willReturn(marathon)

        val exception = assertThrows<CustomException> {
            registrationService.create(1L, request)
        }

        assertEquals(ErrorCode.REGISTRATION_PERIOD_INVALID, exception.errorCode)
    }

    @Test
    @DisplayName("접수 생성 실패 - 취소된 대회이면 MARATHON_ALREADY_CANCELED 예외가 발생한다")
    fun create_fail_marathon_not_open() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(1),
            status = MarathonStatus.CANCELED
        )
        val course = createCourse(marathon = marathon, capacity = 100, currentCount = 10)
        val request = createRegistrationRequest()

        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(courseRepository.findById(1L)).willReturn(Optional.of(course))
        given(marathonRepository.findByIdForUpdate(2L)).willReturn(marathon)

        val exception = assertThrows<CustomException> {
            registrationService.create(1L, request)
        }

        assertEquals(ErrorCode.MARATHON_ALREADY_CANCELED, exception.errorCode)
    }

    @Test
    @DisplayName("접수 생성 실패 - 코스 정원이 가득 차면 CAPACITY_FULL 예외가 발생한다")
    fun create_fail_capacity_full() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(1),
            status = MarathonStatus.OPEN
        )
        val course = createCourse(marathon = marathon, capacity = 10, currentCount = 10)
            .also { setField(it, "id", 1L) }
        val request = createRegistrationRequest()

        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(courseRepository.findById(1L)).willReturn(Optional.of(course))
        given(marathonRepository.findByIdForUpdate(2L)).willReturn(marathon)
        given(courseRepository.increaseCurrentCountIfNotFull(1L)).willReturn(0)

        val exception = assertThrows<CustomException> {
            registrationService.create(1L, request)
        }

        assertEquals(ErrorCode.CAPACITY_FULL, exception.errorCode)
    }

    @Test
    @DisplayName("접수 취소 성공 - 삭제와 코스 인원 감소가 정상 동작한다")
    fun cancel_success() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(1),
            status = MarathonStatus.OPEN
        ).also { setField(it, "id", 2L) }
        val course = createCourse(marathon = marathon, capacity = 100, currentCount = 10)
            .also { setField(it, "id", 3L) }
        val registration = createRegistration(
            user = user,
            course = course,
            marathon = marathon,
            request = createRegistrationRequest()
        ).also {
            setField(it, "id", 1L)
            setField(it, "appliedAt", LocalDateTime.of(2026, 4, 15, 10, 0))
        }

        given(registrationRepository.findById(1L)).willReturn(Optional.of(registration))
        given(paymentService.cancelPaymentForRegistration(1L, "사용자 접수 취소"))
            .willReturn(PaymentCancelResult.NOT_FOUND)

        registrationService.cancel(1L, 1L)

        verify(registrationCancelHistoryRepository, times(1))
            .saveAndFlush(any(RegistrationCancelHistory::class.java))
        verify(courseRepository, times(1)).decreaseCurrentCountIfPositive(3L)
        verify(registrationRepository, times(1)).delete(registration)
    }

    @Test
    @DisplayName("접수 취소 실패 - 접수 내역이 없으면 REGISTRATION_NOT_FOUND 예외가 발생한다")
    fun cancel_fail_registration_not_found() {
        given(registrationRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows<CustomException> {
            registrationService.cancel(1L, 1L)
        }

        assertEquals(ErrorCode.REGISTRATION_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("접수 취소 실패 - 본인 접수 건이 아니면 FORBIDDEN 예외가 발생한다")
    fun cancel_fail_forbidden() {
        val user = createUser(id = 2L, name = "김철수", phoneNumber = "010-2222-3333")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(1),
            status = MarathonStatus.OPEN
        )
        val course = createCourse(marathon = marathon, capacity = 100, currentCount = 10)
        val registration = createRegistration(
            user = user,
            course = course,
            marathon = marathon,
            request = createRegistrationRequest()
        )

        given(registrationRepository.findById(1L)).willReturn(Optional.of(registration))

        val exception = assertThrows<CustomException> {
            registrationService.cancel(1L, 1L)
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    @DisplayName("접수 취소 실패 - 접수 마감 이후면 REGISTRATION_CANCEL_PERIOD_INVALID 예외가 발생한다")
    fun cancel_fail_after_registration_end() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(2),
            registrationEndAt = LocalDateTime.now().minusDays(1),
            status = MarathonStatus.OPEN
        )
        val course = createCourse(marathon = marathon, capacity = 100, currentCount = 10)
        val registration = createRegistration(
            user = user,
            course = course,
            marathon = marathon,
            request = createRegistrationRequest()
        )

        given(registrationRepository.findById(1L)).willReturn(Optional.of(registration))

        val exception = assertThrows<CustomException> {
            registrationService.cancel(1L, 1L)
        }

        assertEquals(ErrorCode.REGISTRATION_CANCEL_PERIOD_INVALID, exception.errorCode)
    }

    @Test
    @DisplayName("접수 취소 실패 - 취소된 대회이면 MARATHON_ALREADY_CANCELED 예외가 발생한다")
    fun cancel_fail_marathon_not_open() {
        val user = createUser(id = 1L, name = "홍길동", phoneNumber = "010-1111-2222")
        val marathon = createMarathon(
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(1),
            status = MarathonStatus.CANCELED
        )
        val course = createCourse(marathon = marathon, capacity = 100, currentCount = 10)
        val registration = createRegistration(
            user = user,
            course = course,
            marathon = marathon,
            request = createRegistrationRequest()
        )

        given(registrationRepository.findById(1L)).willReturn(Optional.of(registration))

        val exception = assertThrows<CustomException> {
            registrationService.cancel(1L, 1L)
        }

        assertEquals(ErrorCode.MARATHON_ALREADY_CANCELED, exception.errorCode)
    }

    private fun createRegistrationRequest(
        courseId: Long = 1L,
        snapZipCode: String = "12345",
        snapAddress: String = "서울시 강남구",
        snapDetail: String? = "101동",
        tSize: String = "L",
        agreedTerms: Boolean = true,
    ) = CreateRegistrationReq(
        courseId = courseId,
        snapZipCode = snapZipCode,
        snapAddress = snapAddress,
        snapDetail = snapDetail,
        tSize = tSize,
        agreedTerms = agreedTerms
    )

    private fun createRegistration(
        user: Users,
        course: Course,
        marathon: Marathon,
        request: CreateRegistrationReq,
    ): Registration = Registration.createCompleted(
        user = user,
        course = course,
        marathon = marathon,
        snapZipCode = request.snapZipCode,
        snapAddress = request.snapAddress,
        snapDetail = request.snapDetail,
        tSize = request.tSize,
        agreedTerms = request.agreedTerms
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
        ).also {
            setField(it, "id", 2L)
            setField(it, "status", status)
        }

    private fun createCourse(
        marathon: Marathon,
        capacity: Int,
        currentCount: Int,
        price: BigDecimal = BigDecimal.ZERO,
    ): Course =
        Course.create(
            courseType = "10K",
            price = price,
            capacity = capacity,
            currentCount = currentCount
        ).also { marathon.addCourse(it) }

    private fun setField(target: Any, name: String, value: Any?) {
        ReflectionTestUtils.setField(target, name, value)
    }
}
