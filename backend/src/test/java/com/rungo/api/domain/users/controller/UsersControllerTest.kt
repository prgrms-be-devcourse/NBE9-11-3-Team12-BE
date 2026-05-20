package com.rungo.api.domain.users.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.rungo.api.domain.users.dto.MyProfileRes
import com.rungo.api.domain.users.dto.UpdateMyProfileReq
import com.rungo.api.domain.users.dto.UpdateMyProfileRes
import com.rungo.api.domain.users.enumtype.Gender.MALE
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.service.UsersService
import com.rungo.api.global.security.SecurityUser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UsersController::class)
class UsersControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var userService: UsersService

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun setAuthenticatedUser(userId: Long) {
        val securityUser = SecurityUser(
            userId,
            "test@test.com",
            Role.PARTICIPANT,
            listOf(SimpleGrantedAuthority("ROLE_PARTICIPANT"))
        )

        val authentication = UsernamePasswordAuthenticationToken(
            securityUser,
            null,
            securityUser.authorities
        )

        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    @DisplayName("내 정보 조회 성공 - 인증된 사용자가 요청하면 200과 사용자 정보를 반환한다")
    fun getMyInfo_success() {
        setAuthenticatedUser(1L)

        val res = MyProfileRes(
            1L, "test@test.com", "홍길동", "010-1234-5678",
            MALE, LocalDate.of(1999, 1, 1), Role.PARTICIPANT
        )

        given(userService.getMyInfo(1L)).willReturn(res)

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value("test@test.com"))
            .andExpect(jsonPath("$.data.name").value("홍길동"))

        verify(userService).getMyInfo(1L)
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 정상 요청 시 200과 수정된 정보를 반환한다")
    fun updateMyProfile_success() {
        setAuthenticatedUser(1L)

        val req = UpdateMyProfileReq("김철수", "010-9999-8888")
        val res = UpdateMyProfileRes("김철수", "010-9999-8888")

        given(userService.updateMyProfile(1L, req)).willReturn(res)

        mockMvc.perform(
            patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("김철수"))
            .andExpect(jsonPath("$.data.phoneNumber").value("010-9999-8888"))

        verify(userService).updateMyProfile(1L, req)
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 모든 필드가 null이면 400을 반환한다")
    fun updateMyProfile_fail_all_null() {
        setAuthenticatedUser(1L)

        val req = UpdateMyProfileReq(null, null)

        mockMvc.perform(
            patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)

        verifyNoInteractions(userService)
    }
}