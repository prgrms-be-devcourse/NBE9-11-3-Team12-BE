package com.rungo.api.global.security

import com.rungo.api.domain.auth.handler.OAuth2AuthenticationFailureHandler
import com.rungo.api.domain.auth.handler.OAuth2AuthenticationSuccessHandler
import com.rungo.api.domain.auth.service.CustomOAuth2UserService
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.global.config.SecurityConfig
import com.rungo.api.global.security.support.TestSecurityController
import com.rungo.api.global.util.JwtUtil
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [TestSecurityController::class])
@Import(SecurityConfig::class, CustomAuthenticationFilter::class)
@TestPropertySource(properties = ["jwt.secret=itistestsecretkeyforjwtabcdefghijklmnopqrstuvwxyz"])
class CustomAuthenticationFilterTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var customOAuth2UserService: CustomOAuth2UserService

    @MockitoBean
    private lateinit var oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler

    @MockitoBean
    private lateinit var oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler

    companion object {
        private const val JWT_SECRET_KEY = "itistestsecretkeyforjwtabcdefghijklmnopqrstuvwxyz"
        private const val COOKIE_NAME = "accessToken"
    }

    private fun validToken(id: Long, email: String, role: Role): String =
        JwtUtil.generateToken(JWT_SECRET_KEY, 3600, mapOf("id" to id, "email" to email, "role" to role.name))

    private fun expiredToken(id: Long, email: String, role: Role): String =
        JwtUtil.generateToken(JWT_SECRET_KEY, -1, mapOf("id" to id, "email" to email, "role" to role.name))

    @Test
    @DisplayName("유효한 accessToken 쿠키가 있으면 id, email, role을 정상 파싱한다.")
    fun validToken_parsesClaimsCorrectly() {
        val token = validToken(1L, "user@test.com", Role.PARTICIPANT)

        mockMvc.perform(get("/test/me").cookie(Cookie(COOKIE_NAME, token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("user@test.com"))
            .andExpect(jsonPath("$.role").value("PARTICIPANT"))
    }

    @Test
    @DisplayName("파싱된 클레임으로 SecurityUser가 올바르게 생성되는지 검증한다.")
    fun validToken_createsSecurityUserCorrectly() {
        val token = validToken(42L, "organizer@test.com", Role.ORGANIZER)

        mockMvc.perform(get("/test/me").cookie(Cookie(COOKIE_NAME, token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.email").value("organizer@test.com"))
            .andExpect(jsonPath("$.role").value("ORGANIZER"))
    }

    @Test
    @DisplayName("SecurityContext에 Authentication이 저장됐는지 검증한다. 저장이 안 됐다면 null이 되어 401을 반환한다.")
    fun validToken_setsAuthenticationInSecurityContext() {
        val token = validToken(1L, "user@test.com", Role.PARTICIPANT)

        mockMvc.perform(get("/test/me").cookie(Cookie(COOKIE_NAME, token)))
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("만료된 토큰으로 요청했을 때 SecurityContext에 인증 정보를 저장하지 않는다.")
    fun expiredToken_doesNotSetAuthentication() {
        val token = expiredToken(1L, "user@test.com", Role.PARTICIPANT)

        mockMvc.perform(get("/test/me").cookie(Cookie(COOKIE_NAME, token)))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("서명이 변조된 토큰으로 요청했을 때 인증 정보가 저장되지 않는다.")
    fun tamperedToken_doesNotSetAuthentication() {
        val token = validToken(1L, "user@test.com", Role.PARTICIPANT) + "tampered"

        mockMvc.perform(get("/test/me").cookie(Cookie(COOKIE_NAME, token)))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("쿠키 자체가 존재하지 않을 때 인증 정보가 저장되지 않는다.")
    fun noCookie_doesNotSetAuthentication() {
        mockMvc.perform(get("/test/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("role 값이 ROLE_ 접두사를 붙여 GrantedAuthority로 매핑된다.")
    fun role_mappedToGrantedAuthority() {
        val token = validToken(1L, "admin@test.com", Role.ADMIN)

        mockMvc.perform(get("/test/me").cookie(Cookie(COOKIE_NAME, token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("ADMIN"))
    }

    @Test
    @DisplayName("PARTICIPANT가 ORGANIZER 전용 API 호출 시 403을 반환한다.")
    fun participantAccessOrganizerApi_returns403() {
        val token = validToken(1L, "user@test.com", Role.PARTICIPANT)

        mockMvc.perform(post("/api/v1/marathons").cookie(Cookie(COOKIE_NAME, token)))
            .andExpect(status().isForbidden)
    }
}