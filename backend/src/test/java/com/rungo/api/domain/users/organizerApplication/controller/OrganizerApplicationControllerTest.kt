package com.rungo.api.domain.users.organizerApplication.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.organizerApplication.dto.OrganizerApplicationCreateReq
import com.rungo.api.domain.users.organizerApplication.dto.OrganizerApplicationCreateRes
import com.rungo.api.domain.users.organizerApplication.service.OrganizerApplicationService
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(OrganizerApplicationController::class)
class OrganizerApplicationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var organizerApplicationService: OrganizerApplicationService

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("주최자 권한 신청 성공 - 정상 요청 시 201과 신청 정보를 반환한다")
    fun requestApplication_success() {
        setAuthenticatedUser(1L)

        val req = OrganizerApplicationCreateReq(
            businessRegistrationNumber = "123-45-67890",
        )

        val res = OrganizerApplicationCreateRes(
            id = 10L,
            userId = 1L,
            businessRegistrationNumber = "123-45-67890",
            status = ApplicationStatus.PENDING,
            requestedAt = LocalDateTime.of(2026, 5, 19, 12, 0),
        )

        given(organizerApplicationService.requestApplication(1L, req))
            .willReturn(res)

        mockMvc.perform(
            post("/api/v1/organizer-applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("주최자 권한신청 성공"))
            .andExpect(jsonPath("$.data.id").value(10))
            .andExpect(jsonPath("$.data.userId").value(1))
            .andExpect(jsonPath("$.data.businessRegistrationNumber").value("123-45-67890"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.requestedAt").value("2026-05-19T12:00:00"))

        verify(organizerApplicationService).requestApplication(1L, req)
    }

    @Test
    @DisplayName("주최자 권한 신청 실패 - 사업자등록번호가 비어 있으면 400을 반환한다")
    fun requestApplication_fail_businessRegistrationNumber_blank() {
        setAuthenticatedUser(1L)

        val req = OrganizerApplicationCreateReq(
            businessRegistrationNumber = "",
        )

        mockMvc.perform(
            post("/api/v1/organizer-applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)

        verifyNoInteractions(organizerApplicationService)
    }

    private fun setAuthenticatedUser(userId: Long) {
        val securityUser = SecurityUser(
            userId,
            "user@test.com",
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
}
