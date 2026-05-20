package com.rungo.api.domain.users.service

import com.rungo.api.domain.users.dto.UpdateMyProfileReq
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class UsersServiceTest {

    @InjectMocks
    private lateinit var usersService: UsersService

    @Mock
    private lateinit var usersRepository: UserRepository

    @Test
    @DisplayName("내 정보 조회 성공 - 유효한 userId로 조회하면 사용자 정보를 반환한다")
    fun getMyInfo_success() {
        val user = createUser()

        given(usersRepository.findById(1L)).willReturn(Optional.of(user))

        val res = usersService.getMyInfo(1L)

        assertNotNull(res)
        assertEquals(1L, res.id)
        assertEquals("test@test.com", res.email)
        assertEquals("홍길동", res.name)
        assertEquals("010-1234-5678", res.phoneNumber)
        assertEquals(Gender.MALE, res.gender)
        assertEquals(Role.PARTICIPANT, res.role)
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 존재하지 않는 userId면 USER_NOT_FOUND 예외가 발생한다")
    fun getMyInfo_fail_user_not_found() {
        given(usersRepository.findById(anyLong())).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            usersService.getMyInfo(999L)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 이름과 전화번호를 모두 수정하면 변경된 정보를 반환한다")
    fun updateMyProfile_success_both_fields() {
        val user = createUser()
        val req = UpdateMyProfileReq("김철수", "010-9999-8888")

        given(usersRepository.findById(1L)).willReturn(Optional.of(user))

        val res = usersService.updateMyProfile(1L, req)

        assertEquals("김철수", res.name)
        assertEquals("010-9999-8888", res.phoneNumber)
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 이름만 수정하면 전화번호는 기존 값이 유지된다")
    fun updateMyProfile_success_only_name() {
        val user = createUser()
        val req = UpdateMyProfileReq("김철수", null)

        given(usersRepository.findById(1L)).willReturn(Optional.of(user))

        val res = usersService.updateMyProfile(1L, req)

        assertEquals("김철수", res.name)
        assertEquals("010-1234-5678", res.phoneNumber)
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 전화번호만 수정하면 이름은 기존 값이 유지된다")
    fun updateMyProfile_success_only_phoneNumber() {
        val user = createUser()
        val req = UpdateMyProfileReq(null, "010-9999-8888")

        given(usersRepository.findById(1L)).willReturn(Optional.of(user))

        val res = usersService.updateMyProfile(1L, req)

        assertEquals("홍길동", res.name)
        assertEquals("010-9999-8888", res.phoneNumber)
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 존재하지 않는 userId면 USER_NOT_FOUND 예외가 발생한다")
    fun updateMyProfile_fail_user_not_found() {
        val req = UpdateMyProfileReq("김철수", "010-1234-5678")

        given(usersRepository.findById(anyLong())).willReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            usersService.updateMyProfile(999L, req)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    private fun createUser(): Users {
        val user = Users.create(
            "test@test.com",
            "홍길동",
            "010-1234-5678",
            Gender.MALE,
            LocalDate.of(1999, 1, 1)
        )

        ReflectionTestUtils.setField(user, "id", 1L)

        return user
    }
}