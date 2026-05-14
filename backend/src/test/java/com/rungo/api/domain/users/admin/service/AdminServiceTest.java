package com.rungo.api.domain.users.admin.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("주최자 권한 부여 성공 - 관리자가 참가자 유저를 ORGANIZER로 승급시킨다")
    void approve_organizer_success() {
        Users admin = createUser(1L, "관리자", Role.ADMIN);
        Users participant = createUser(2L, "참가자", Role.PARTICIPANT);

        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(userRepository.findById(2L)).willReturn(Optional.of(participant));

        adminService.approveOrganizer(1L, 2L);

        assertEquals(Role.ORGANIZER, participant.getRole());
    }

    @Test
    @DisplayName("주최자 권한 부여 실패 - 관리자 유저가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void approve_organizer_fail_admin_not_found() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> adminService.approveOrganizer(1L, 2L)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("주최자 권한 부여 실패 - 관리자가 아니면 FORBIDDEN 예외가 발생한다")
    void approve_organizer_fail_not_admin() {
        Users participant = createUser(1L, "참가자", Role.PARTICIPANT);

        given(userRepository.findById(1L)).willReturn(Optional.of(participant));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> adminService.approveOrganizer(1L, 2L)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("주최자 권한 부여 실패 - 대상 유저가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void approve_organizer_fail_target_user_not_found() {
        Users admin = createUser(1L, "관리자", Role.ADMIN);

        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> adminService.approveOrganizer(1L, 2L)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("주최자 권한 부여 실패 - 이미 주최자면 ALREADY_ORGANIZER 예외가 발생한다")
    void approve_organizer_fail_already_organizer() {
        Users admin = createUser(1L, "관리자", Role.ADMIN);
        Users organizer = createUser(2L, "주최자", Role.ORGANIZER);

        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(userRepository.findById(2L)).willReturn(Optional.of(organizer));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> adminService.approveOrganizer(1L, 2L)
        );

        assertEquals(ErrorCode.ALREADY_ORGANIZER, exception.getErrorCode());
    }

    private Users createUser(Long id, String name, Role role) {
        return Users.builder()
                .id(id)
                .email(name + "@test.com")
                .name(name)
                .phoneNumber("010-1111-2222")
                .role(role)
                .gender(Gender.MALE)
                .birth(LocalDate.of(2000, 1, 1))
                .build();
    }
}