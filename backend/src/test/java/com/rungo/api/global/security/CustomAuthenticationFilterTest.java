package com.rungo.api.global.security;

import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.auth.handler.OAuth2AuthenticationFailureHandler;
import com.rungo.api.domain.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.rungo.api.domain.auth.service.CustomOAuth2UserService;
import com.rungo.api.global.config.SecurityConfig;
import com.rungo.api.global.security.support.TestSecurityController;
import com.rungo.api.global.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestSecurityController.class)
@Import({SecurityConfig.class, CustomAuthenticationFilter.class})
@TestPropertySource(properties = "jwt.secret=itistestsecretkeyforjwtabcdefghijklmnopqrstuvwxyz") // 필터가 사용
class CustomAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private static final String JWT_SECRET_KEY = "itistestsecretkeyforjwtabcdefghijklmnopqrstuvwxyz"; // 토큰 만들 때 사용
    private static final String COOKIE_NAME = "accessToken";

    private String validToken(Long id, String email, Role role) {
        return JwtUtil.generateToken(JWT_SECRET_KEY, 3600,
                Map.of("id", id, "email", email, "role", role.name()));
    }

    private String expiredToken(Long id, String email, Role role) {
        return JwtUtil.generateToken(JWT_SECRET_KEY, -1,
                Map.of("id", id, "email", email, "role", role.name()));
    }

    @Test
    @DisplayName("유효한 accessToken 쿠키가 있으면 id, email, role을 정상 파싱한다.")
    void validToken_parsesClaimsCorrectly() throws Exception {
        String token = validToken(1L, "user@test.com", Role.PARTICIPANT);

        mockMvc.perform(get("/test/me")
                        .cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.role").value("PARTICIPANT"));
    }

    @Test
    @DisplayName("파싱된 클레임으로 SecurityUser가 올바르게 생성되는지 검증한다.")
    void validToken_createsSecurityUserCorrectly() throws Exception {
        String token = validToken(42L, "organizer@test.com", Role.ORGANIZER);

        mockMvc.perform(get("/test/me")
                        .cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("organizer@test.com"))
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    @DisplayName("SecurityContext에 Authentication이 저장됐는지 검증한다. 저장이 안 됐다면 null이 되어 401을 반환한다.")
    void validToken_setsAuthenticationInSecurityContext() throws Exception {
        String token = validToken(1L, "user@test.com", Role.PARTICIPANT);

        mockMvc.perform(get("/test/me")
                        .cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("만료된 토큰으로 요청했을 때 SecurityContext에 인증 정보를 저장하지 않는다.")
    void expiredToken_doesNotSetAuthentication() throws Exception {
        String token = expiredToken(1L, "user@test.com", Role.PARTICIPANT);

        mockMvc.perform(get("/test/me")
                        .cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("서명이 변조된 토큰으로 요청했을 때 인증 정보가 저장되지 않는다.")
    void tamperedToken_doesNotSetAuthentication() throws Exception {
        String token = validToken(1L, "user@test.com", Role.PARTICIPANT) + "tampered";

        mockMvc.perform(get("/test/me")
                        .cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("쿠키 자체가 존재하지 않을 때 인증 정보가 저장되지 않는다.")
    void noCookie_doesNotSetAuthentication() throws Exception {
        mockMvc.perform(get("/test/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("role 값이 ROLE_ 접두사를 붙여 GrantedAuthority로 매핑된다.")
    void role_mappedToGrantedAuthority() throws Exception {

        String token = validToken(1L, "admin@test.com", Role.ADMIN);

        mockMvc.perform(get("/test/me")
                        .cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("PARTICIPANT가 ORGANIZER 전용 API 호출 시 403을 반환한다.")
    void participantAccessOrganizerApi_returns403() throws Exception {
        String token = validToken(1L, "user@test.com", Role.PARTICIPANT);

        mockMvc.perform(post("/api/v1/marathons")
                        .cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isForbidden());
    }
}
