package com.rungo.api.domain.users.admin.service

import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    @InjectMocks
    private lateinit var adminService: AdminService

    @Mock
    private lateinit var userRepository: UserRepository

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

    private fun createUser(id: Long, name: String, role: Role): Users {
        val user = Users.create(
            "$name@test.com",
            name,
            "010-1111-2222",
            Gender.MALE,
            LocalDate.of(2000, 1, 1)
        )

        if (role == Role.ORGANIZER || role == Role.ADMIN) {
            ReflectionTestUtils.setField(user, "role", role)
        }

        ReflectionTestUtils.setField(user, "id", id)

        return user
    }
}