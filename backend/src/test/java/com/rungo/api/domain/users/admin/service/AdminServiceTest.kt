package com.rungo.api.domain.users.admin.service

import com.rungo.api.domain.users.admin.dto.RejectOrganizerApplicationReq
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Role
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
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    private lateinit var adminService: AdminService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var organizerApplicationRepository: OrganizerApplicationRepository

    @BeforeEach
    fun setUp() {
        adminService = AdminService(
            userRepository = userRepository,
            organizerApplicationRepository = organizerApplicationRepository,
        )
    }

    @Test
    @DisplayName("주최자 권한 부여 성공 - 관리자가 참가자 유저를 ORGANIZER로 승급시킨다")
    fun approve_organizer_success() {
        val admin = createUser(1L, "관리자", Role.ADMIN)
        val participant = createUser(2L, "참가자", Role.PARTICIPANT)

        given(userRepository.findById(1L)).willReturn(Optional.of(admin))
        given(userRepository.findById(2L)).willReturn(Optional.of(participant))

        adminService.approveOrganizer(1L, 2L)

        assertEquals(Role.ORGANIZER, participant.role)
    }

    @Test
    @DisplayName("주최자 권한 부여 실패 - 관리자 유저가 없으면 USER_NOT_FOUND 예외가 발생한다")
    fun approve_organizer_fail_admin_not_found() {
        given(userRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            adminService.approveOrganizer(1L, 2L)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 권한 부여 실패 - 관리자가 아니면 FORBIDDEN 예외가 발생한다")
    fun approve_organizer_fail_not_admin() {
        val participant = createUser(1L, "참가자", Role.PARTICIPANT)

        given(userRepository.findById(1L)).willReturn(Optional.of(participant))

        val exception = assertThrows(CustomException::class.java) {
            adminService.approveOrganizer(1L, 2L)
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 권한 부여 실패 - 대상 유저가 없으면 USER_NOT_FOUND 예외가 발생한다")
    fun approve_organizer_fail_target_user_not_found() {
        val admin = createUser(1L, "관리자", Role.ADMIN)

        given(userRepository.findById(1L)).willReturn(Optional.of(admin))
        given(userRepository.findById(2L)).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            adminService.approveOrganizer(1L, 2L)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 권한 부여 실패 - 이미 주최자면 ALREADY_ORGANIZER 예외가 발생한다")
    fun approve_organizer_fail_already_organizer() {
        val admin = createUser(1L, "관리자", Role.ADMIN)
        val organizer = createUser(2L, "주최자", Role.ORGANIZER)

        given(userRepository.findById(1L)).willReturn(Optional.of(admin))
        given(userRepository.findById(2L)).willReturn(Optional.of(organizer))

        val exception = assertThrows(CustomException::class.java) {
            adminService.approveOrganizer(1L, 2L)
        }

        assertEquals(ErrorCode.ALREADY_ORGANIZER, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 권한 신청 거절 성공 - 신청 상태가 REJECTED로 변경되고 거절 사유가 저장된다")
    fun rejectOrganizerApplicationSuccess() {
        val admin = createUser(1L, "관리자", Role.ADMIN)
        val applicant = createUser(2L, "참가자", Role.PARTICIPANT)
        val application = createApplication(
            id = 10L,
            user = applicant,
            status = ApplicationStatus.PENDING,
        )

        val req = RejectOrganizerApplicationReq(
            rejectReason = "사업자등록번호 확인이 필요합니다.",
        )

        given(userRepository.findById(1L)).willReturn(Optional.of(admin))
        given(organizerApplicationRepository.findById(10L)).willReturn(Optional.of(application))

        val result = adminService.rejectOrganizerApplication(
            adminId = 1L,
            applicationId = 10L,
            req = req,
        )

        assertEquals(10L, result.applicationId)
        assertEquals(2L, result.userId)
        assertEquals(ApplicationStatus.REJECTED, result.status)
        assertEquals("사업자등록번호 확인이 필요합니다.", result.rejectReason)

        assertEquals(ApplicationStatus.REJECTED, application.status)
        assertEquals("사업자등록번호 확인이 필요합니다.", application.rejectReason)
        assertEquals(Role.PARTICIPANT, applicant.role)

        verify(userRepository).findById(1L)
        verify(organizerApplicationRepository).findById(10L)
    }

    @Test
    @DisplayName("주최자 권한 신청 거절 실패 - 관리자가 존재하지 않으면 USER_NOT_FOUND 예외가 발생한다")
    fun rejectOrganizerApplicationFailAdminNotFound() {
        val req = RejectOrganizerApplicationReq(
            rejectReason = "사업자등록번호 확인이 필요합니다.",
        )

        given(userRepository.findById(1L)).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            adminService.rejectOrganizerApplication(
                adminId = 1L,
                applicationId = 10L,
                req = req,
            )
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 권한 신청 거절 실패 - 관리자 권한이 아니면 FORBIDDEN 예외가 발생한다")
    fun rejectOrganizerApplicationFailNotAdmin() {
        val user = createUser(1L, "참가자", Role.PARTICIPANT)

        val req = RejectOrganizerApplicationReq(
            rejectReason = "사업자등록번호 확인이 필요합니다.",
        )

        given(userRepository.findById(1L)).willReturn(Optional.of(user))

        val exception = assertThrows(CustomException::class.java) {
            adminService.rejectOrganizerApplication(
                adminId = 1L,
                applicationId = 10L,
                req = req,
            )
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 권한 신청 거절 실패 - 신청 내역이 없으면 ORGANIZER_APPLICATION_NOT_FOUND 예외가 발생한다")
    fun rejectOrganizerApplicationFailApplicationNotFound() {
        val admin = createUser(1L, "관리자", Role.ADMIN)

        val req = RejectOrganizerApplicationReq(
            rejectReason = "사업자등록번호 확인이 필요합니다.",
        )

        given(userRepository.findById(1L)).willReturn(Optional.of(admin))
        given(organizerApplicationRepository.findById(10L)).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            adminService.rejectOrganizerApplication(
                adminId = 1L,
                applicationId = 10L,
                req = req,
            )
        }

        assertEquals(ErrorCode.ORGANIZER_APPLICATION_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("주최자 권한 신청 거절 실패 - 이미 처리된 신청이면 ALREADY_PROCESSED_APPLICATION 예외가 발생한다")
    fun rejectOrganizerApplicationFailAlreadyProcessed() {
        val admin = createUser(1L, "관리자", Role.ADMIN)
        val applicant = createUser(2L, "참가자", Role.PARTICIPANT)
        val application = createApplication(
            id = 10L,
            user = applicant,
            status = ApplicationStatus.APPROVED,
        )

        val req = RejectOrganizerApplicationReq(
            rejectReason = "사업자등록번호 확인이 필요합니다.",
        )

        given(userRepository.findById(1L)).willReturn(Optional.of(admin))
        given(organizerApplicationRepository.findById(10L)).willReturn(Optional.of(application))

        val exception = assertThrows(CustomException::class.java) {
            adminService.rejectOrganizerApplication(
                adminId = 1L,
                applicationId = 10L,
                req = req,
            )
        }

        assertEquals(ErrorCode.ALREADY_PROCESSED_APPLICATION, exception.errorCode)
    }

    private fun createApplication(
        id: Long,
        user: Users,
        status: ApplicationStatus,
    ): OrganizerApplication {
        val application = OrganizerApplication.create(
            user = user,
            businessRegistrationNumber = "123-45-67890",
        )

        ReflectionTestUtils.setField(application, "id", id)
        ReflectionTestUtils.setField(application, "status", status)

        return application
    }

    private fun createUser(
        id: Long,
        name: String,
        role: Role,
    ): Users {
        val user = Users.create(
            "$name@test.com",
            name,
            "010-1111-2222",
            Gender.MALE,
            LocalDate.of(2000, 1, 1),
        )

        ReflectionTestUtils.setField(user, "id", id)
        ReflectionTestUtils.setField(user, "role", role)

        return user
    }
}
