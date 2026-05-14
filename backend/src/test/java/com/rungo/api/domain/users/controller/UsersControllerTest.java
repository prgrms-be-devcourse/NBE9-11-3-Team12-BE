package com.rungo.api.domain.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rungo.api.domain.users.dto.MyProfileRes;
import com.rungo.api.domain.users.dto.UpdateMyProfileReq;
import com.rungo.api.domain.users.dto.UpdateMyProfileRes;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.service.UsersService;
import com.rungo.api.global.security.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static com.rungo.api.domain.users.enumtype.Gender.MALE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UsersController.class)
class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsersService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityUser securityUser = new SecurityUser(
                userId,
                "test@test.com",
                Role.PARTICIPANT,
                List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT"))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        securityUser,
                        null,
                        securityUser.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // 내 정보 조회 테스트
    @Test
    @DisplayName("내 정보 조회 성공 - 인증된 사용자가 요청하면 200과 사용자 정보를 반환한다")
    void getMyInfo_success() throws Exception {
        setAuthenticatedUser(1L);

        MyProfileRes res = new MyProfileRes(
                1L, "test@test.com", "홍길동", "010-1234-5678",
                MALE, LocalDate.of(1999, 1, 1), Role.PARTICIPANT
        );

        given(userService.getMyInfo(1L)).willReturn(res);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"));

        verify(userService).getMyInfo(1L);
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 인증되지 않은 사용자가 요청하면 401을 반환한다")
    void getMyInfo_fail_unauthorized() throws Exception {

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    // 내 정보 수정 테스트
    @Test
    @DisplayName("내 정보 수정 성공 - 정상 요청 시 200과 수정된 정보를 반환한다")
    void updateMyProfile_success() throws Exception {
        setAuthenticatedUser(1L);

        UpdateMyProfileReq req = new UpdateMyProfileReq("김철수", "010-9999-8888", Gender.MALE, LocalDate.of(1999, 1, 1));

        UpdateMyProfileRes res = new UpdateMyProfileRes(
                1L,
                "test@test.com",
                "김철수",
                "010-9999-8888",
                Gender.MALE,
                LocalDate.of(1999, 1, 1),
                Role.PARTICIPANT
        );

        given(userService.updateMyProfile(1L, req)).willReturn(res);

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("김철수"))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-9999-8888"));

        verify(userService).updateMyProfile(1L, req);
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 인증되지 않은 사용자가 요청하면 401을 반환한다")
    void updateMyProfile_fail_unauthorized() throws Exception {
        UpdateMyProfileReq req = new UpdateMyProfileReq("김철수", null, Gender.MALE, LocalDate.of(1999, 1, 1));

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 모든 필드가 null이면 400을 반환한다")
    void updateMyProfile_fail_all_null() throws Exception {
        setAuthenticatedUser(1L);

        UpdateMyProfileReq req = new UpdateMyProfileReq(null, null, null, null);

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}
