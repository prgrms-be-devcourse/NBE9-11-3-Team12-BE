package com.rungo.api.domain.marathon.marathon.service

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonReq
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonReq.CreateCourseItemReq
import com.rungo.api.domain.marathon.marathon.dto.read.ReadMyMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonReq
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonReq.UpdateCourseItemReq
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.notification.event.MarathonCanceledEvent
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.file.FileStorageService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationEventPublisher
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class MarathonServiceTest {

    private lateinit var marathonService: MarathonService

    @Mock
    private lateinit var marathonRepository: MarathonRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var registrationRepository: RegistrationRepository

    @Mock
    private lateinit var registrationCancelHistoryRepository: RegistrationCancelHistoryRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var fileStorageService: FileStorageService

    @BeforeEach
    fun setUp() {
        marathonService = MarathonService(
            marathonRepository,
            userRepository,
            registrationRepository,
            registrationCancelHistoryRepository,
            eventPublisher,
            fileStorageService,
            1L,
            1L,
        )
    }

    @Test
    @DisplayName("대회 취소 성공 시 참가자들에게 취소 알림 이벤트를 발행한다")
    fun cancelMarathonPublishEventSuccess() {
        val organizer = createUser(1L, "주최자", Role.ORGANIZER)
        val marathon = Marathon.create(
            organizer,
            "서울 마라톤",
            "서울",
            "성동구",
            LocalDate.of(2026, 10, 3),
            "poster.png",
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1),
        )

        ReflectionTestUtils.setField(marathon, "id", 1L)

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer))
        given(marathonRepository.findById(1L)).willReturn(Optional.of(marathon))
        given(registrationRepository.findParticipantEmailsByMarathonId(1L))
            .willReturn(listOf("user1@test.com", "user2@test.com"))

        marathonService.cancelMarathon(1L, 1L)

        verify(eventPublisher, times(1))
            .publishEvent(any(MarathonCanceledEvent::class.java))
    }

    @Test
    @DisplayName("마라톤 생성 성공 - 저장과 응답 반환 및 코스 정규화가 정상 동작한다")
    fun createSuccess() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)

        val request = CreateMarathonReq(
            title = "서울 마라톤",
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.of(2026, 10, 3),
            posterImage = posterImage("poster.png"),
            registrationStartAt = LocalDateTime.of(2026, 8, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 8, 31, 18, 0),
            courses = listOf(
                CreateCourseItemReq("5k", BigDecimal("30000"), 100),
                CreateCourseItemReq("10K", BigDecimal("50000"), 200),
            ),
        )

        given(userRepository.findById(organizerId)).willReturn(Optional.of(organizer))
        given(fileStorageService.saveMarathonPoster(any(MultipartFile::class.java)))
            .willReturn("poster.png")

        val now = LocalDateTime.of(2026, 7, 1, 12, 0)

        given(marathonRepository.save(any(Marathon::class.java)))
            .willAnswer { invocation ->
                val saved = invocation.getArgument<Marathon>(0)

                ReflectionTestUtils.setField(saved, "id", 10L)
                ReflectionTestUtils.setField(saved, "createdAt", now)
                ReflectionTestUtils.setField(saved.courses[0], "id", 101L)
                ReflectionTestUtils.setField(saved.courses[1], "id", 102L)

                saved
            }

        val result = marathonService.createMarathon(organizerId, request)

        val marathonCaptor = ArgumentCaptor.forClass(Marathon::class.java)
        verify(marathonRepository, times(1)).save(marathonCaptor.capture())

        val capturedMarathon = marathonCaptor.value

        assertSame(organizer, capturedMarathon.organizer)
        assertEquals("서울 마라톤", capturedMarathon.title)
        assertEquals("서울", capturedMarathon.region)
        assertEquals(LocalDate.of(2026, 10, 3), capturedMarathon.eventDate)
        assertEquals("poster.png", capturedMarathon.posterImageUrl)
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), capturedMarathon.registrationStartAt)
        assertEquals(LocalDateTime.of(2026, 8, 31, 18, 0), capturedMarathon.registrationEndAt)
        assertEquals(MarathonStatus.OPEN, capturedMarathon.status)

        assertEquals(2, capturedMarathon.courses.size)
        assertEquals("5K", capturedMarathon.courses[0].courseType)
        assertEquals(BigDecimal("30000"), capturedMarathon.courses[0].price)
        assertEquals(100, capturedMarathon.courses[0].capacity)
        assertEquals(0, capturedMarathon.courses[0].currentCount)
        assertEquals("10K", capturedMarathon.courses[1].courseType)
        assertEquals(BigDecimal("50000"), capturedMarathon.courses[1].price)
        assertEquals(200, capturedMarathon.courses[1].capacity)
        assertEquals(0, capturedMarathon.courses[1].currentCount)

        assertNotNull(result)
        assertEquals(10L, result.id)
        assertEquals("서울 마라톤", result.title)
        assertEquals("서울", result.region)
        assertEquals(LocalDate.of(2026, 10, 3), result.eventDate)
        assertEquals("poster.png", result.posterImageUrl)
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), result.registrationStartAt)
        assertEquals(LocalDateTime.of(2026, 8, 31, 18, 0), result.registrationEndAt)
        assertEquals(MarathonStatus.OPEN, result.status)
        assertEquals(2, result.courses.size)
        assertEquals(101L, result.courses[0].id)
        assertEquals("5K", result.courses[0].courseType)
        assertEquals(102L, result.courses[1].id)
        assertEquals("10K", result.courses[1].courseType)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    fun createFailUserNotFound() {
        val organizerId = 1L
        val request = createMarathonReq()

        given(userRepository.findById(organizerId)).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            marathonService.createMarathon(organizerId, request)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - 주최자 권한이 아니면 FORBIDDEN 예외가 발생한다")
    fun createFailNotOrganizer() {
        val organizerId = 1L
        val participant = createUser(organizerId, "참가자", Role.PARTICIPANT)
        val request = createMarathonReq()

        given(userRepository.findById(organizerId)).willReturn(Optional.of(participant))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.createMarathon(organizerId, request)
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - 접수 시작일이 종료일보다 늦으면 INVALID_INPUT_VALUE 예외가 발생한다")
    fun createFailRegistrationPeriodInvalid() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)

        val request = createMarathonReq(
            registrationStartAt = LocalDateTime.of(2026, 9, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 8, 31, 18, 0),
        )

        given(userRepository.findById(organizerId)).willReturn(Optional.of(organizer))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.createMarathon(organizerId, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - 개최일이 접수 종료일보다 이르면 INVALID_INPUT_VALUE 예외가 발생한다")
    fun createFailEventDateInvalid() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)

        val request = createMarathonReq(
            eventDate = LocalDate.of(2026, 8, 20),
        )

        given(userRepository.findById(organizerId)).willReturn(Optional.of(organizer))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.createMarathon(organizerId, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - 코스 타입이 정규화 후 중복되면 INVALID_INPUT_VALUE 예외가 발생한다")
    fun createFailDuplicateCourseType() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)

        val request = createMarathonReq(
            courses = listOf(
                CreateCourseItemReq("5k", BigDecimal("30000"), 100),
                CreateCourseItemReq(" 5K ", BigDecimal("50000"), 200),
            )
        )

        given(userRepository.findById(organizerId)).willReturn(Optional.of(organizer))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.createMarathon(organizerId, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - 접수 시작일과 종료일 간격이 1일 미만이면 예외 발생")
    fun createFailStartEndInterval() {
        val organizer = createUser(1L, "주최자", Role.ORGANIZER)

        val request = createMarathonReq(
            eventDate = LocalDate.of(2026, 8, 5),
            registrationStartAt = LocalDateTime.of(2026, 8, 1, 10, 0),
            registrationEndAt = LocalDateTime.of(2026, 8, 1, 15, 0),
        )

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.createMarathon(1L, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - 접수 종료일과 대회일 간격이 1일 미만이면 예외 발생")
    fun createFailEndEventInterval() {
        val organizer = createUser(1L, "주최자", Role.ORGANIZER)

        val request = createMarathonReq(
            eventDate = LocalDate.of(2026, 8, 4),
            registrationStartAt = LocalDateTime.of(2026, 8, 1, 10, 0),
            registrationEndAt = LocalDateTime.of(2026, 8, 4, 15, 0),
        )

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.createMarathon(1L, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 상세 조회 실패 - 취소된 대회면 MARATHON_CANCELED 예외 발생")
    fun getMarathonDetailFailCanceled() {
        val canceledMarathon = Marathon.create(
            createUser(1L, "이순신", Role.ORGANIZER),
            "서울 마라톤",
            "서울",
            "성동구",
            LocalDate.of(2026, 10, 3),
            "poster.png",
            LocalDateTime.now().minusDays(10),
            LocalDateTime.now().minusDays(5),
        )

        ReflectionTestUtils.setField(canceledMarathon, "status", MarathonStatus.CANCELED)

        given(marathonRepository.findById(1L)).willReturn(Optional.of(canceledMarathon))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.getMarathonDetail(1L)
        }

        assertEquals(ErrorCode.MARATHON_CANCELED, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 취소 성공 - 본인 대회를 취소하고 CANCELING 상태로 변경한다")
    fun cancelMarathonSuccess() {
        val organizer = createUser(1L, "주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, organizer, MarathonStatus.OPEN)

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer))
        given(marathonRepository.findById(10L)).willReturn(Optional.of(marathon))
        given(registrationRepository.findParticipantEmailsByMarathonId(10L)).willReturn(emptyList())
        given(registrationRepository.findAllByMarathon_IdOrderByAppliedAtDesc(10L)).willReturn(emptyList())

        val result = marathonService.cancelMarathon(1L, 10L)

        assertNotNull(result)
        assertEquals(10L, result.marathonId)
        assertEquals("서울 마라톤", result.title)
        assertEquals(LocalDate.of(2026, 10, 3), result.eventDate)
        assertEquals(MarathonStatus.CANCELED, result.status)
        assertEquals(MarathonStatus.CANCELED, marathon.status)
        assertEquals(2, result.courses.size)
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 주최자 유저가 없으면 USER_NOT_FOUND 예외가 발생한다")
    fun cancelMarathonFailUserNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            marathonService.cancelMarathon(1L, 10L)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 주최자 권한이 아니면 FORBIDDEN 예외가 발생한다")
    fun cancelMarathonFailNotOrganizer() {
        val participant = createUser(1L, "참가자", Role.PARTICIPANT)

        given(userRepository.findById(1L)).willReturn(Optional.of(participant))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.cancelMarathon(1L, 10L)
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 존재하지 않는 대회면 MARATHON_NOT_FOUND 예외가 발생한다")
    fun cancelMarathonFailMarathonNotFound() {
        val organizer = createUser(1L, "주최자", Role.ORGANIZER)

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer))
        given(marathonRepository.findById(10L)).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            marathonService.cancelMarathon(1L, 10L)
        }

        assertEquals(ErrorCode.MARATHON_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 본인 대회가 아니면 FORBIDDEN 예외가 발생한다")
    fun cancelMarathonFailForbidden() {
        val organizer = createUser(1L, "주최자", Role.ORGANIZER)
        val anotherOrganizer = createUser(2L, "다른주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, anotherOrganizer, MarathonStatus.OPEN)

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer))
        given(marathonRepository.findById(10L)).willReturn(Optional.of(marathon))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.cancelMarathon(1L, 10L)
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 이미 취소된 대회면 MARATHON_ALREADY_CANCELED 예외가 발생한다")
    fun cancelMarathonFailAlreadyCanceled() {
        val organizer = createUser(1L, "주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, organizer, MarathonStatus.CANCELED)

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer))
        given(marathonRepository.findById(10L)).willReturn(Optional.of(marathon))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.cancelMarathon(1L, 10L)
        }

        assertEquals(ErrorCode.MARATHON_ALREADY_CANCELED, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 수정 성공 - 대회 기본 정보와 코스 정보가 정상 수정된다")
    fun updateMarathonSuccess() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, organizer, MarathonStatus.OPEN)

        val request = updateMarathonReq(
            title = "수정된 서울 마라톤",
            region = "부산",
            detailedAddress = "중구",
            eventDate = LocalDate.of(2026, 11, 15),
            posterImage = posterImage("updated-poster.png"),
            registrationStartAt = LocalDateTime.of(2026, 9, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 9, 30, 18, 0),
            courses = listOf(
                UpdateCourseItemReq(101L, "HALF", BigDecimal.valueOf(40000), 150),
                UpdateCourseItemReq(102L, "FULL", BigDecimal.valueOf(70000), 300),
            ),
        )

        given(
            marathonRepository.findByIdAndOrganizer_Id(10L, organizerId)
        ).willReturn(marathon)
        given(fileStorageService.saveMarathonPoster(any(MultipartFile::class.java)))
            .willReturn("updated-poster.png")

        val result = marathonService.updateMarathon(organizerId, 10L, request)

        assertNotNull(result)
        assertEquals(10L, result.id)
        assertEquals("수정된 서울 마라톤", result.title)
        assertEquals("부산", result.region)
        assertEquals(LocalDate.of(2026, 11, 15), result.eventDate)
        assertEquals("updated-poster.png", result.posterImageUrl)
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 0), result.registrationStartAt)
        assertEquals(LocalDateTime.of(2026, 9, 30, 18, 0), result.registrationEndAt)
        assertEquals(2, result.courses.size)
        assertEquals("HALF", result.courses[0].courseType)
        assertEquals(BigDecimal.valueOf(40000), result.courses[0].price)
        assertEquals(150, result.courses[0].capacity)
        assertEquals("FULL", result.courses[1].courseType)
        assertEquals(BigDecimal.valueOf(70000), result.courses[1].price)
        assertEquals(300, result.courses[1].capacity)
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 본인 대회가 아니거나 존재하지 않으면 MARATHON_NOT_FOUND 예외가 발생한다")
    fun updateMarathonFailNotFound() {
        val organizerId = 1L
        val request = updateMarathonReq()

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
            .willReturn(null)

        val exception = assertThrows(CustomException::class.java) {
            marathonService.updateMarathon(organizerId, 10L, request)
        }

        assertEquals(ErrorCode.MARATHON_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 접수가 이미 시작된 대회면 INVALID_INPUT_VALUE 예외가 발생한다")
    fun updateMarathonFailRegistrationStarted() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)
        val marathon = Marathon.create(
            organizer,
            "서울 마라톤",
            "서울",
            "성동구",
            LocalDate.of(2026, 10, 3),
            "poster.png",
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(5),
        )

        val request = updateMarathonReq()

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
            .willReturn(marathon)

        val exception = assertThrows(CustomException::class.java) {
            marathonService.updateMarathon(organizerId, 10L, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 이미 취소된 대회면 MARATHON_ALREADY_CANCELED 예외가 발생한다")
    fun updateMarathonFailAlreadyCanceled() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, organizer, MarathonStatus.CANCELED)
        val request = updateMarathonReq()

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
            .willReturn(marathon)

        val exception = assertThrows(CustomException::class.java) {
            marathonService.updateMarathon(organizerId, 10L, request)
        }

        assertEquals(ErrorCode.MARATHON_ALREADY_CANCELED, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 접수 시작일이 종료일보다 늦으면 INVALID_INPUT_VALUE 예외가 발생한다")
    fun updateMarathonFailRegistrationPeriodInvalid() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, organizer, MarathonStatus.OPEN)

        val request = updateMarathonReq(
            registrationStartAt = LocalDateTime.of(2026, 9, 30, 18, 0),
            registrationEndAt = LocalDateTime.of(2026, 9, 1, 9, 0),
        )

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
            .willReturn(marathon)

        val exception = assertThrows(CustomException::class.java) {
            marathonService.updateMarathon(organizerId, 10L, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 개최일이 접수 종료일보다 이르면 INVALID_INPUT_VALUE 예외가 발생한다")
    fun updateMarathonFailEventDateInvalid() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, organizer, MarathonStatus.OPEN)

        val request = updateMarathonReq(
            eventDate = LocalDate.of(2026, 9, 10),
        )

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
            .willReturn(marathon)

        val exception = assertThrows(CustomException::class.java) {
            marathonService.updateMarathon(organizerId, 10L, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 코스 ID가 중복되면 INVALID_INPUT_VALUE 예외가 발생한다")
    fun updateMarathonFailDuplicateCourseIds() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, organizer, MarathonStatus.OPEN)

        val request = updateMarathonReq(
            courses = listOf(
                UpdateCourseItemReq(101L, "HALF", BigDecimal.valueOf(40000), 150),
                UpdateCourseItemReq(101L, "FULL", BigDecimal.valueOf(70000), 300),
            )
        )

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
            .willReturn(marathon)

        val exception = assertThrows(CustomException::class.java) {
            marathonService.updateMarathon(organizerId, 10L, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 존재하지 않는 코스를 수정하려 하면 COURSE_NOT_FOUND 예외가 발생한다")
    fun updateMarathonFailCourseNotFound() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, organizer, MarathonStatus.OPEN)

        val request = updateMarathonReq(
            courses = listOf(
                UpdateCourseItemReq(999L, "HALF", BigDecimal.valueOf(40000), 150),
            )
        )

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
            .willReturn(marathon)

        val exception = assertThrows(CustomException::class.java) {
            marathonService.updateMarathon(organizerId, 10L, request)
        }

        assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 코스 타입이 정규화 후 중복되면 INVALID_INPUT_VALUE 예외가 발생한다")
    fun updateMarathonFailDuplicateCourseType() {
        val organizerId = 1L
        val organizer = createUser(organizerId, "주최자", Role.ORGANIZER)
        val marathon = createMarathon(10L, organizer, MarathonStatus.OPEN)

        val request = updateMarathonReq(
            courses = listOf(
                UpdateCourseItemReq(101L, " 10k ", BigDecimal.valueOf(40000), 150),
                UpdateCourseItemReq(102L, "10K", BigDecimal.valueOf(70000), 300),
            )
        )

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
            .willReturn(marathon)

        val exception = assertThrows(CustomException::class.java) {
            marathonService.updateMarathon(organizerId, 10L, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 내 대회 조회 성공 - 본인이 주최한 대회 목록을 반환한다")
    fun getMyMarathonsSuccess() {
        val organizer = createUser(1L, "주최자", Role.ORGANIZER)
        val marathon1 = createMarathon(10L, organizer, MarathonStatus.OPEN)
        val marathon2 = createMarathon(11L, organizer, MarathonStatus.CANCELING)

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer))
        given(
            marathonRepository.findByOrganizerIdAndStatusNotIn(
                1L,
                listOf(MarathonStatus.CANCELING, MarathonStatus.CANCELED),
            )
        ).willReturn(listOf(marathon1, marathon2))

        val result: List<ReadMyMarathonRes> = marathonService.getMyMarathons(1L)

        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals(10L, result[0].id)
        assertEquals("서울 마라톤", result[0].title)
        assertEquals("서울", result[0].region)
        assertEquals(MarathonStatus.OPEN, result[0].status)
        assertEquals(2, result[0].courses.size)
        assertEquals("5K", result[0].courses[0].courseType)
        assertEquals("10K", result[0].courses[1].courseType)
        assertEquals(11L, result[1].id)
        assertEquals(MarathonStatus.CANCELING, result[1].status)
    }

    @Test
    @DisplayName("주최자 내 대회 조회 실패 - 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    fun getMyMarathonsFailUserNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            marathonService.getMyMarathons(1L)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 내 대회 조회 실패 - 주최자 권한이 아니면 FORBIDDEN 예외가 발생한다")
    fun getMyMarathonsFailNotOrganizer() {
        val participant = createUser(1L, "참가자", Role.PARTICIPANT)

        given(userRepository.findById(1L)).willReturn(Optional.of(participant))

        val exception = assertThrows(CustomException::class.java) {
            marathonService.getMyMarathons(1L)
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    private fun createMarathonReq(
        title: String = "서울 마라톤",
        region: String = "서울",
        detailedAddress: String = "성동구",
        eventDate: LocalDate = LocalDate.of(2026, 10, 3),
        posterImage: MockMultipartFile = posterImage("poster.png"),
        registrationStartAt: LocalDateTime = LocalDateTime.of(2026, 8, 1, 9, 0),
        registrationEndAt: LocalDateTime = LocalDateTime.of(2026, 8, 31, 18, 0),
        courses: List<CreateCourseItemReq> = listOf(
            CreateCourseItemReq("5K", BigDecimal("30000"), 100),
        ),
    ): CreateMarathonReq =
        CreateMarathonReq(
            title,
            region,
            detailedAddress,
            eventDate,
            posterImage,
            registrationStartAt,
            registrationEndAt,
            courses,
        )

    private fun updateMarathonReq(
        title: String = "수정된 서울 마라톤",
        region: String = "부산",
        detailedAddress: String = "중구",
        eventDate: LocalDate = LocalDate.of(2026, 11, 15),
        posterImage: MockMultipartFile = posterImage("updated-poster.png"),
        registrationStartAt: LocalDateTime = LocalDateTime.of(2026, 9, 1, 9, 0),
        registrationEndAt: LocalDateTime = LocalDateTime.of(2026, 9, 30, 18, 0),
        courses: List<UpdateCourseItemReq> = emptyList(),
    ): UpdateMarathonReq =
        UpdateMarathonReq(
            title,
            region,
            detailedAddress,
            eventDate,
            posterImage,
            registrationStartAt,
            registrationEndAt,
            courses,
        )

    private fun createUser(id: Long, name: String, role: Role): Users {
        val user = Users.create(
            "test@test.com",
            name,
            "010-1111-2222",
            Gender.MALE,
            LocalDate.of(2000, 1, 1),
        )

        if (role == Role.ORGANIZER) {
            user.promoteToOrganizer()
        }

        ReflectionTestUtils.setField(user, "id", id)

        return user
    }

    private fun posterImage(originalFilename: String): MockMultipartFile =
        MockMultipartFile(
            "posterImage",
            originalFilename,
            "image/png",
            "poster".toByteArray(),
        )

    private fun createMarathon(
        id: Long,
        organizer: Users,
        status: MarathonStatus,
    ): Marathon {
        val marathon = Marathon.create(
            organizer,
            "서울 마라톤",
            "서울",
            "성동구",
            LocalDate.of(2026, 10, 3),
            "poster.png",
            LocalDateTime.of(2026, 8, 1, 9, 0),
            LocalDateTime.of(2026, 8, 31, 18, 0),
        )

        val course1 = Course.create(
            "5K",
            BigDecimal.valueOf(30000),
            100,
            0,
        )

        val course2 = Course.create(
            "10K",
            BigDecimal.valueOf(50000),
            200,
            0,
        )

        marathon.addCourse(course1)
        marathon.addCourse(course2)

        ReflectionTestUtils.setField(marathon, "id", id)
        ReflectionTestUtils.setField(marathon, "status", status)
        ReflectionTestUtils.setField(course1, "id", 101L)
        ReflectionTestUtils.setField(course2, "id", 102L)

        return marathon
    }
}
