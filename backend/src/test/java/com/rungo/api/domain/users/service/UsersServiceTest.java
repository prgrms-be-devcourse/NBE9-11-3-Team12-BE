package com.rungo.api.domain.users.service;

import com.rungo.api.domain.users.dto.MyProfileRes;
import com.rungo.api.domain.users.dto.UpdateMyProfileReq;
import com.rungo.api.domain.users.dto.UpdateMyProfileRes;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @InjectMocks
    private UsersService usersService;

    @Mock
    private UserRepository usersRepository;

    // 내 정보 조회 테스트
    @Test
    @DisplayName("내 정보 조회 성공 - 유효한 userId로 조회하면 사용자 정보를 반환한다")
    void getMyInfo_success() {
        Users user = Users.builder()
                .id(1L)
                .email("test@test.com")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1999, 1, 1))
                .role(Role.PARTICIPANT)
                .build();

        given(usersRepository.findById(1L)).willReturn(Optional.of(user));

        MyProfileRes res = usersService.getMyInfo(1L);

        assertNotNull(res);
        assertEquals(1L, res.id());
        assertEquals("test@test.com", res.email());
        assertEquals("홍길동", res.name());
        assertEquals("010-1234-5678", res.phoneNumber());
        assertEquals(Gender.MALE, res.gender());
        assertEquals(Role.PARTICIPANT, res.role());
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 존재하지 않는 userId면 USER_NOT_FOUND 예외가 발생한다")
    void getMyInfo_fail_user_not_found() {
        given(usersRepository.findById(anyLong())).willReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> usersService.getMyInfo(999L));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    // 내 정보 수정 테스트
    @Test
    @DisplayName("내 정보 수정 성공 - 이름과 전화번호를 모두 수정하면 변경된 정보를 반환한다")
    void updateMyProfile_success_both_fields() {
        Users user = Users.builder()
                .id(1L)
                .email("test@test.com")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1999, 1, 1))
                .role(Role.PARTICIPANT)
                .build();

        UpdateMyProfileReq req = new UpdateMyProfileReq("김철수", "010-9999-8888", Gender.MALE, LocalDate.of(1999, 1, 1));

        given(usersRepository.findById(1L)).willReturn(Optional.of(user));

        UpdateMyProfileRes res = usersService.updateMyProfile(1L, req);

        assertEquals("김철수", res.name());
        assertEquals("010-9999-8888", res.phoneNumber());
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 이름만 수정하면 전화번호는 기존 값이 유지된다")
    void updateMyProfile_success_only_name() {
        Users user = Users.builder()
                .id(1L)
                .email("test@test.com")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1999, 1, 1))
                .role(Role.PARTICIPANT)
                .build();

        UpdateMyProfileReq req = new UpdateMyProfileReq("김철수", null, Gender.MALE, LocalDate.of(1999, 1, 1));

        given(usersRepository.findById(1L)).willReturn(Optional.of(user));

        UpdateMyProfileRes res = usersService.updateMyProfile(1L, req);

        assertEquals("김철수", res.name());
        assertEquals("010-1234-5678", res.phoneNumber());
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 전화번호만 수정하면 이름은 기존 값이 유지된다")
    void updateMyProfile_success_only_phoneNumber() {
        Users user = Users.builder()
                .id(1L)
                .email("test@test.com")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1999, 1, 1))
                .role(Role.PARTICIPANT)
                .build();

        UpdateMyProfileReq req = new UpdateMyProfileReq(null, "010-9999-8888", Gender.MALE, LocalDate.of(1999, 1, 1));

        given(usersRepository.findById(1L)).willReturn(Optional.of(user));

        UpdateMyProfileRes res = usersService.updateMyProfile(1L, req);

        assertEquals("홍길동", res.name());
        assertEquals("010-9999-8888", res.phoneNumber());
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 존재하지 않는 userId면 USER_NOT_FOUND 예외가 발생한다")
    void updateMyProfile_fail_user_not_found() {
        UpdateMyProfileReq req = new UpdateMyProfileReq("김철수", "010-1234-5678", Gender.MALE, LocalDate.of(1999, 1, 1));

        given(usersRepository.findById(anyLong())).willReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> usersService.updateMyProfile(999L, req));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
