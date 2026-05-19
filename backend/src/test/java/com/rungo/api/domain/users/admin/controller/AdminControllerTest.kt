package com.rungo.api.domain.users.admin.controller

import com.rungo.api.domain.users.admin.service.AdminService
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.global.security.SecurityUser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
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

    @MockitoBean
    private lateinit var adminService: AdminService

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