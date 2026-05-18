package com.rungo.api.global.security

import com.rungo.api.domain.auth.handler.OAuth2AuthenticationFailureHandler
import com.rungo.api.domain.auth.handler.OAuth2AuthenticationSuccessHandler
import com.rungo.api.domain.auth.service.CustomOAuth2UserService
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.global.config.SecurityConfig
import com.rungo.api.global.security.support.TestSecurityController
import com.rungo.api.global.util.JwtUtil.generateToken
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.Map

@WebMvcTest(controllers = [TestSecurityController::class])
@Import(SecurityConfig::class, CustomAuthenticationFilter::class)
@TestPropertySource(properties = ["jwt.secret=itistestsecretkeyforjwtabcdefghijklmnopqrstuvwxyz"])
internal class CustomAuthenticationFilterTest {
    @Autowired
    private val mockMvc: MockMvc? = null

    @MockitoBean
    private val customOAuth2UserService: CustomOAuth2UserService? = null

    @MockitoBean
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler? = null

    @MockitoBean
    private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler? = null

    private fun validToken(id: Long, email: String, role: Role): String {
        return generateToken(
            JWT_SECRET_KEY, 3600,
            Map.of("id", id, "email", email, "role", role.name)
        )
    }

    private fun expiredToken(id: Long, email: String, role: Role): String {
        return generateToken(
            JWT_SECRET_KEY, -1,
            Map.of("id", id, "email", email, "role", role.name)
        )
    }

    @Test
    @DisplayName("유효한 accessToken 쿠키가 있으면 id, email, role을 정상 파싱한다.")
    @Throws(Exception::class)
    fun validToken_parsesClaimsCorrectly() {
        val token = validToken(1L, "user@test.com", Role.PARTICIPANT)

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/test/me")
                .cookie(Cookie(COOKIE_NAME, token))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("user@test.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("PARTICIPANT"))
    }

    @Test
    @DisplayName("파싱된 클레임으로 SecurityUser가 올바르게 생성되는지 검증한다.")
    @Throws(Exception::class)
    fun validToken_createsSecurityUserCorrectly() {
        val token = validToken(42L, "organizer@test.com", Role.ORGANIZER)

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/test/me")
                .cookie(Cookie(COOKIE_NAME, token))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(42))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("organizer@test.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("ORGANIZER"))
    }

    @Test
    @DisplayName("SecurityContext에 Authentication이 저장됐는지 검증한다. 저장이 안 됐다면 null이 되어 401을 반환한다.")
    @Throws(Exception::class)
    fun validToken_setsAuthenticationInSecurityContext() {
        val token = validToken(1L, "user@test.com", Role.PARTICIPANT)

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/test/me")
                .cookie(Cookie(COOKIE_NAME, token))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
    }

    @Test
    @DisplayName("만료된 토큰으로 요청했을 때 SecurityContext에 인증 정보를 저장하지 않는다.")
    @Throws(Exception::class)
    fun expiredToken_doesNotSetAuthentication() {
        val token = expiredToken(1L, "user@test.com", Role.PARTICIPANT)

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/test/me")
                .cookie(Cookie(COOKIE_NAME, token))
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    @Test
    @DisplayName("서명이 변조된 토큰으로 요청했을 때 인증 정보가 저장되지 않는다.")
    @Throws(Exception::class)
    fun tamperedToken_doesNotSetAuthentication() {
        val token = validToken(1L, "user@test.com", Role.PARTICIPANT) + "tampered"

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/test/me")
                .cookie(Cookie(COOKIE_NAME, token))
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    @Test
    @DisplayName("쿠키 자체가 존재하지 않을 때 인증 정보가 저장되지 않는다.")
    @Throws(Exception::class)
    fun noCookie_doesNotSetAuthentication() {
        mockMvc!!.perform(MockMvcRequestBuilders.get("/test/me"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    @Test
    @DisplayName("role 값이 ROLE_ 접두사를 붙여 GrantedAuthority로 매핑된다.")
    @Throws(Exception::class)
    fun role_mappedToGrantedAuthority() {
        val token = validToken(1L, "admin@test.com", Role.ADMIN)

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/test/me")
                .cookie(Cookie(COOKIE_NAME, token))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("ADMIN"))
    }

    @Test
    @DisplayName("PARTICIPANT가 ORGANIZER 전용 API 호출 시 403을 반환한다.")
    @Throws(Exception::class)
    fun participantAccessOrganizerApi_returns403() {
        val token = validToken(1L, "user@test.com", Role.PARTICIPANT)

        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/marathons")
                .cookie(Cookie(COOKIE_NAME, token))
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    companion object {
        private const val JWT_SECRET_KEY = "itistestsecretkeyforjwtabcdefghijklmnopqrstuvwxyz" // 토큰 만들 때 사용
        private const val COOKIE_NAME = "accessToken"
    }
}
