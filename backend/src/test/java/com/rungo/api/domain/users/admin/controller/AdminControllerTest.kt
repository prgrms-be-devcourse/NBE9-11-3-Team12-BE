package com.rungo.api.domain.users.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.rungo.api.domain.marathon.marathon.service.MarathonCleanupService
import com.rungo.api.domain.users.admin.dto.RejectOrganizerApplicationReq
import com.rungo.api.domain.users.admin.dto.RejectOrganizerApplicationRes
import com.rungo.api.domain.users.admin.service.AdminService
import com.rungo.api.domain.users.enumtype.Role
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AdminController::class)
class AdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var adminService: AdminService

    @MockitoBean
    private lateinit var marathonCleanupService: MarathonCleanupService


    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("주최자 권한 부여 성공 - 200 상태코드와 공통 응답 규격을 반환한다")
    fun approve_organizer_success() {
        setAuthenticatedAdmin(1L)

        mockMvc.perform(patch("/api/v1/admin/{userId}/organizer", 2L))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))

        verify(adminService).approveOrganizer(1L, 2L)
    }

    @Test
    @DisplayName("주최자 권한 신청 거절 성공 - 200 상태코드와 거절 응답을 반환한다")
    fun rejectOrganizerApplicationSuccess() {
        setAuthenticatedAdmin(1L)

        val req = RejectOrganizerApplicationReq(
            rejectReason = "사업자등록번호 확인이 필요합니다.",
        )

        val res = RejectOrganizerApplicationRes(
            applicationId = 10L,
            userId = 2L,
            status = ApplicationStatus.REJECTED,
            rejectReason = "사업자등록번호 확인이 필요합니다.",
        )

        given(adminService.rejectOrganizerApplication(1L, 10L, req))
            .willReturn(res)

        mockMvc.perform(
            patch("/api/v1/admin/organizer-applications/{applicationId}/reject", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.applicationId").value(10))
            .andExpect(jsonPath("$.data.userId").value(2))
            .andExpect(jsonPath("$.data.status").value("REJECTED"))
            .andExpect(jsonPath("$.data.rejectReason").value("사업자등록번호 확인이 필요합니다."))

        verify(adminService).rejectOrganizerApplication(1L, 10L, req)
    }

    @Test
    @DisplayName("주최자 권한 신청 거절 실패 - 거절 사유가 비어 있으면 400을 반환한다")
    fun rejectOrganizerApplicationFailRejectReasonBlank() {
        setAuthenticatedAdmin(1L)

        val req = RejectOrganizerApplicationReq(
            rejectReason = "",
        )

        mockMvc.perform(
            patch("/api/v1/admin/organizer-applications/{applicationId}/reject", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isBadRequest)

        verifyNoInteractions(adminService)
    }

    private fun setAuthenticatedAdmin(userId: Long) {
        val securityUser = SecurityUser(
            userId,
            "admin@test.com",
            Role.ADMIN,
            listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )

        val authentication = UsernamePasswordAuthenticationToken(
            securityUser,
            null,
            securityUser.authorities
        )

        SecurityContextHolder.getContext().authentication = authentication
    }
}