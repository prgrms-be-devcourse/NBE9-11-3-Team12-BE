package com.rungo.api.domain.users.organizerApplication.service

import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.entity.Users.Companion.create
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.organizerApplication.dto.OrganizerApplicationCreateReq
import com.rungo.api.domain.users.organizerApplication.entity.OrganizerApplication
import com.rungo.api.domain.users.organizerApplication.repository.OrganizerApplicationRepository
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.stubbing.Answer
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class OrganizerApplicationServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var organizerApplicationRepository: OrganizerApplicationRepository

    private lateinit var organizerApplicationService: OrganizerApplicationService

    @BeforeEach
    fun setUp() {
        organizerApplicationService = OrganizerApplicationService(
            userRepository = userRepository,
            organizerApplicationRepository = organizerApplicationRepository,
        )
    }

    @Test
    @DisplayName("주최자 권한 신청 성공 - PENDING 상태의 신청 내역을 생성한다")
    fun requestApplicationSuccess() {
        val user = createUser(id = 1L, role = Role.PARTICIPANT)

        val request = OrganizerApplicationCreateReq(
            businessRegistrationNumber = "123-45-67890",
        )

        given(userRepository.findById(1L))
            .willReturn(Optional.of(user))

        given(
            organizerApplicationRepository.existsByUserIdAndStatus(
                1L,
                ApplicationStatus.PENDING,
            )
        ).willReturn(false)

        given(organizerApplicationRepository.save(any(OrganizerApplication::class.java)))
            .willAnswer(Answer { invocation ->
                val application = invocation.getArgument<OrganizerApplication>(0)
                ReflectionTestUtils.setField(application, "id", 10L)
                ReflectionTestUtils.setField(application, "requestedAt", LocalDateTime.of(2026, 5, 19, 12, 0))
                application
            })

        val result = organizerApplicationService.requestApplication(
            userId = 1L,
            req = request,
        )

        assertEquals(10L, result.id)
        assertEquals(1L, result.userId)
        assertEquals("123-45-67890", result.businessRegistrationNumber)
        assertEquals(ApplicationStatus.PENDING, result.status)
        assertEquals(LocalDateTime.of(2026, 5, 19, 12, 0), result.requestedAt)

        verify(userRepository).findById(1L)
        verify(organizerApplicationRepository).existsByUserIdAndStatus(
            1L,
            ApplicationStatus.PENDING,
        )
        verify(organizerApplicationRepository).save(any(OrganizerApplication::class.java))
    }

    @Test
    @DisplayName("주최자 권한 신청 실패 - 사용자가 존재하지 않으면 USER_NOT_FOUND 예외가 발생한다")
    fun requestApplicationFailUserNotFound() {
        val request = OrganizerApplicationCreateReq(
            businessRegistrationNumber = "123-45-67890",
        )

        given(userRepository.findById(1L))
            .willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            organizerApplicationService.requestApplication(
                userId = 1L,
                req = request,
            )
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 권한 신청 실패 - 이미 주최자 권한을 가진 사용자는 신청할 수 없다")
    fun requestApplicationFailAlreadyOrganizer() {
        val organizer = createUser(id = 1L, role = Role.ORGANIZER)

        val request = OrganizerApplicationCreateReq(
            businessRegistrationNumber = "123-45-67890",
        )

        given(userRepository.findById(1L))
            .willReturn(Optional.of(organizer))

        val exception = assertThrows(CustomException::class.java) {
            organizerApplicationService.requestApplication(
                userId = 1L,
                req = request,
            )
        }

        assertEquals(ErrorCode.ALREADY_ORGANIZER, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 권한 신청 실패 - 이미 대기 중인 신청이 있으면 중복 신청할 수 없다")
    fun requestApplicationFailPendingApplicationExists() {
        val user = createUser(id = 1L, role = Role.PARTICIPANT)

        val request = OrganizerApplicationCreateReq(
            businessRegistrationNumber = "123-45-67890",
        )

        given(userRepository.findById(1L))
            .willReturn(Optional.of(user))

        given(
            organizerApplicationRepository.existsByUserIdAndStatus(
                1L,
                ApplicationStatus.PENDING,
            )
        ).willReturn(true)

        val exception = assertThrows(CustomException::class.java) {
            organizerApplicationService.requestApplication(
                userId = 1L,
                req = request,
            )
        }

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.errorCode)
    }

    private fun createUser(
        id: Long,
        role: Role,
    ): Users {
        val user = create(
            email = "user$id@test.com",
            name = "테스트유저$id",
            phoneNumber = "010-1111-2222",
            gender = Gender.MALE,
            birth = LocalDate.of(2000, 1, 1),
        )

        ReflectionTestUtils.setField(user, "id", id)
        ReflectionTestUtils.setField(user, "role", role)

        return user
    }
}
