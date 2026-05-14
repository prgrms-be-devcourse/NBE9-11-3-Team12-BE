package com.rungo.api.domain.registration.controller;

import com.rungo.api.domain.marathon.marathon.dto.PageRes;
import com.rungo.api.domain.registration.dto.MyRegistrationRes;
import com.rungo.api.domain.registration.enumtype.MyRegistrationStatusFilter;
import com.rungo.api.domain.registration.service.RegistrationService;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.global.security.SecurityUser;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RegistrationController.class)
class RegistrationReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("내 접수 조회 성공 - 기본값으로 ACTIVE, page=0, size=20을 사용해 공통 응답을 반환한다")
    void getMyRegistrations_active_success_with_default_params() throws Exception {
        setAuthenticatedUser(1L);

        MyRegistrationRes response = new MyRegistrationRes(
                List.of(new MyRegistrationRes.Item(
                        10L,
                        null,
                        20L,
                        "서울 마라톤",
                        30L,
                        "10K",
                        "ACTIVE",
                        new BigDecimal("50000"),
                        LocalDate.of(2026, 10, 25),
                        "홍길동",
                        "010-1111-2222",
                        "12345",
                        "서울시 강남구",
                        "101동",
                        "L",
                        true,
                        LocalDateTime.of(2026, 4, 20, 9, 30),
                        null
                )),
                new PageRes(0, 20, 1, 1)
        );

        given(registrationService.getMyRegistrations(
                eq(1L),
                eq(MyRegistrationStatusFilter.ACTIVE),
                eq(PageRequest.of(0, 20))
        )).willReturn(response);

        mockMvc.perform(get("/api/v1/registrations/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.content[0].registrationId").value(10))
                .andExpect(jsonPath("$.data.content[0].historyId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.content[0].marathonId").value(20))
                .andExpect(jsonPath("$.data.content[0].marathonTitle").value("서울 마라톤"))
                .andExpect(jsonPath("$.data.content[0].courseId").value(30))
                .andExpect(jsonPath("$.data.content[0].courseType").value("10K"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.content[0].price").value(50000))
                .andExpect(jsonPath("$.data.content[0].eventDate").value("2026-10-25"))
                .andExpect(jsonPath("$.data.content[0].snapName").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].snapPhoneNumber").value("010-1111-2222"))
                .andExpect(jsonPath("$.data.content[0].snapZipCode").value("12345"))
                .andExpect(jsonPath("$.data.content[0].snapAddress").value("서울시 강남구"))
                .andExpect(jsonPath("$.data.content[0].snapDetail").value("101동"))
                .andExpect(jsonPath("$.data.content[0].tSize").value("L"))
                .andExpect(jsonPath("$.data.content[0].agreedTerms").value(true))
                .andExpect(jsonPath("$.data.content[0].appliedAt").value("2026-04-20T09:30:00"))
                .andExpect(jsonPath("$.data.content[0].canceledAt").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.pageRes.page").value(0))
                .andExpect(jsonPath("$.data.pageRes.size").value(20))
                .andExpect(jsonPath("$.data.pageRes.totalElements").value(1))
                .andExpect(jsonPath("$.data.pageRes.totalPages").value(1));

        verify(registrationService).getMyRegistrations(1L, MyRegistrationStatusFilter.ACTIVE, PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("내 접수 조회 성공 - status=CANCELED이면 취소 접수 목록을 반환한다")
    void getMyRegistrations_canceled_success() throws Exception {
        setAuthenticatedUser(1L);

        MyRegistrationRes response = new MyRegistrationRes(
                List.of(new MyRegistrationRes.Item(
                        40L,
                        10L,
                        20L,
                        "서울 마라톤",
                        30L,
                        "Half",
                        "CANCELED",
                        new BigDecimal("70000"),
                        LocalDate.of(2026, 10, 25),
                        "홍길동",
                        "010-1111-2222",
                        "54321",
                        "서울시 송파구",
                        "202동",
                        "M",
                        true,
                        LocalDateTime.of(2026, 4, 1, 8, 0),
                        LocalDateTime.of(2026, 4, 5, 18, 30)
                )),
                new PageRes(1, 10, 11, 2)
        );

        given(registrationService.getMyRegistrations(
                eq(1L),
                eq(MyRegistrationStatusFilter.CANCELED),
                eq(PageRequest.of(1, 10))
        )).willReturn(response);

        mockMvc.perform(get("/api/v1/registrations/me")
                        .param("status", "CANCELED")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].registrationId").value(40))
                .andExpect(jsonPath("$.data.content[0].historyId").value(10))
                .andExpect(jsonPath("$.data.content[0].status").value("CANCELED"))
                .andExpect(jsonPath("$.data.content[0].canceledAt").value("2026-04-05T18:30:00"))
                .andExpect(jsonPath("$.data.pageRes.page").value(1))
                .andExpect(jsonPath("$.data.pageRes.size").value(10))
                .andExpect(jsonPath("$.data.pageRes.totalElements").value(11))
                .andExpect(jsonPath("$.data.pageRes.totalPages").value(2));

        verify(registrationService).getMyRegistrations(1L, MyRegistrationStatusFilter.CANCELED, PageRequest.of(1, 10));
    }

    @Test
    @DisplayName("내 접수 조회 실패 - page가 0 미만이면 400 INVALID_INPUT_VALUE를 반환한다")
    void getMyRegistrations_fail_invalid_page() throws Exception {
        setAuthenticatedUser(1L);

        mockMvc.perform(get("/api/v1/registrations/me")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.page").exists());

        verifyNoInteractions(registrationService);
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
}