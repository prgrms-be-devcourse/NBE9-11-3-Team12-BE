package com.rungo.api.domain.users.admin.controller;

import com.rungo.api.domain.users.admin.service.AdminService;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import com.rungo.api.global.security.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("주최자 권한 부여 성공 - 200 상태코드와 공통 응답 규격을 반환한다")
    void approve_organizer_success() throws Exception {
        setAuthenticatedAdmin(1L);

        mockMvc.perform(patch("/api/v1/admin/{userId}/organizer", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."));

        verify(adminService).approveOrganizer(1L, 2L);
    }

    //테스트용 관리자 설정 메서드
    private void setAuthenticatedAdmin(Long userId) {
        SecurityUser securityUser = new SecurityUser(
                userId,
                "admin@test.com",
                Role.ADMIN,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        securityUser,
                        null,
                        securityUser.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
