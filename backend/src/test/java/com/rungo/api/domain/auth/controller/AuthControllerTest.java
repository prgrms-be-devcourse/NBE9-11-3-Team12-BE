package com.rungo.api.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rungo.api.domain.auth.dto.*;
import com.rungo.api.domain.auth.service.AuthService;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // 회원가입 테스트
    @Test
    @DisplayName("회원가입 성공 - 201 상태코드와 공통 응답 규격이 반환된다")
    void signup_success() throws Exception {
        SignUpReq req = new SignUpReq(
                "test@test.com", "Password123!", "홍길동",
                "010-1234-5678", Gender.MALE, LocalDate.of(1999, 1, 1)
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("회원가입 성공"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식이 틀리면 400 에러를 반환한다")
    void signup_fail_invalid_email() throws Exception {
        SignUpReq req = new SignUpReq(
                "invalid-email", "Password123!", "홍길동",
                "010-1234-5678", Gender.MALE, LocalDate.of(1999, 1, 1)
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일이면 409 에러를 반환한다")
    void signup_fail_duplicate_email() throws Exception {
        SignUpReq req = new SignUpReq(
                "test@test.com", "Password123!", "홍길동",
                "010-1234-5678", Gender.MALE, LocalDate.of(1999, 1, 1)
        );

        given(authService.signup(any())).willThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // 로그인 테스트
    @Test
    @DisplayName("로그인 성공 - 200 상태코드와 쿠키에 토큰이 담겨 반환된다")
    void login_success() throws Exception {
        LoginReq req = new LoginReq("test@test.com", "Password123!");
        LoginRes loginRes = new LoginRes(1L, "test@test.com", "홍길동", Role.PARTICIPANT);
        LoginResult result = new LoginResult("access-token", "refresh-token", loginRes);

        given(authService.login(any())).willReturn(result);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.email").value("test@test.com")) // JSON 응답 검증
                .andExpect(cookie().exists("accessToken")) // 쿠키 검증
                .andExpect(cookie().maxAge("accessToken", 60 * 60))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().maxAge("refreshToken", 60 * 60 * 24 * 7));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일이면 예외를 반환한다")
    void login_fail_user_not_found() throws Exception {
        LoginReq req = new LoginReq("notfound@test.com", "Password123!");

        given(authService.login(any())).willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호가 틀리면 예외를 반환한다")
    void login_fail_invalid_credentials() throws Exception {
        LoginReq req = new LoginReq("test@test.com", "wrongPassword!");

        given(authService.login(any())).willThrow(new CustomException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // 로그아웃 테스트
    @Test
    @DisplayName("로그아웃 성공 - 유효한 refreshToken 쿠키가 있으면 200을 반환한다")
    void logout_success_with_token() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
    }

    @Test
    @DisplayName("로그아웃 성공 - refreshToken 쿠키가 없는 상태로 요청해도 200을 반환한다")
    void logout_success_without_token() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
    }

    // 토큰 재발급 테스트
    @Test
    @DisplayName("토큰 재발급 성공 - 유효한 refreshToken이면 200과 함께 새 토큰 쿠키가 발급된다")
    void reissue_success() throws Exception {
        TokenRes tokenRes = new TokenRes("new-access-token", "new-refresh-token");

        given(authService.tokenReissue(any())).willReturn(tokenRes);

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().maxAge("accessToken", 60 * 60))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().maxAge("refreshToken", 60 * 60 * 24 * 7));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - refreshToken 쿠키가 없으면 404를 반환한다")
    void reissue_fail_no_token() throws Exception {
        given(authService.tokenReissue(null))
                .willThrow(new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        mockMvc.perform(post("/api/v1/auth/reissue"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 변조되었거나 만료된 refreshToken이면 예외를 반환한다")
    void reissue_fail_invalid_token() throws Exception {
        given(authService.tokenReissue(any()))
                .willThrow(new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new Cookie("refreshToken", "invalid-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis 저장 토큰과 불일치하면 탈취로 감지하여 예외를 반환한다")
    void reissue_fail_token_mismatch() throws Exception {
        given(authService.tokenReissue(any()))
                .willThrow(new CustomException(ErrorCode.TOKEN_MISMATCH));

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new Cookie("refreshToken", "mismatched-token")))
                .andExpect(status().isUnauthorized());
    }
}