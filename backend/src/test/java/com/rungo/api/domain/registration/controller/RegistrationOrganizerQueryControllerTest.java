package com.rungo.api.domain.registration.controller;

import com.rungo.api.domain.registration.dto.RegistrationOverviewRes;
import com.rungo.api.domain.registration.dto.RegistrationParticipantDetailRes;
import com.rungo.api.domain.registration.dto.RegistrationParticipantListRes;
import com.rungo.api.domain.registration.enumtype.RegistrationStatus;
import com.rungo.api.domain.registration.service.RegistrationOrganizerQueryService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@WebMvcTest(RegistrationOrganizerQueryController.class)
class RegistrationOrganizerQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationOrganizerQueryService registrationOrganizerQueryService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("접수 요약 조회 성공 - 200과 공통 응답 규격을 반환한다")
    void getRegistrationOverview_success() throws Exception {
        setAuthenticatedOrganizer(1L);

        RegistrationOverviewRes response = new RegistrationOverviewRes(
                new RegistrationOverviewRes.MarathonInfo(
                        10L,
                        "서울 마라톤",
                        LocalDate.of(2026, 10, 25),
                        "서울",
                        85,
                        300,
                        215,
                        28
                ),
                List.of(
                        new RegistrationOverviewRes.CourseStatus(
                                101L, "10K", new BigDecimal("50000"), 25, 100, 75, 25
                        ),
                        new RegistrationOverviewRes.CourseStatus(
                                102L, "Half", new BigDecimal("70000"), 60, 200, 140, 30
                        )
                )
        );

        given(registrationOrganizerQueryService.getRegistrationOverview(1L, 10L))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/organizer/marathons/10/registrations/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.marathon.marathonId").value(10))
                .andExpect(jsonPath("$.data.marathon.marathonTitle").value("서울 마라톤"))
                .andExpect(jsonPath("$.data.marathon.totalCurrentCount").value(85))
                .andExpect(jsonPath("$.data.marathon.totalCapacity").value(300))
                .andExpect(jsonPath("$.data.courseStatuses[0].courseId").value(101))
                .andExpect(jsonPath("$.data.courseStatuses[0].courseType").value("10K"))
                .andExpect(jsonPath("$.data.courseStatuses[1].courseId").value(102))
                .andExpect(jsonPath("$.data.courseStatuses[1].courseType").value("Half"));

        verify(registrationOrganizerQueryService).getRegistrationOverview(1L, 10L);
    }

    @Test
    @DisplayName("참가자 목록 조회 성공 - page,size,courseId,name을 전달하면 서비스에 그대로 위임한다")
    void getMarathonParticipants_success() throws Exception {
        setAuthenticatedOrganizer(1L);

        RegistrationParticipantListRes response = new RegistrationParticipantListRes(
                List.of(
                        new RegistrationParticipantListRes.Item(
                                1000L,
                                "홍길동",
                                "010-1111-2222",
                                "L",
                                101L,
                                "10K",
                                RegistrationStatus.COMPLETED,
                                LocalDateTime.of(2026, 4, 20, 9, 30)
                        )
                ),
                new com.rungo.api.domain.marathon.marathon.dto.PageRes(0, 10, 1, 1)
        );

        PageRequest expectedPageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Order.desc("appliedAt"), Sort.Order.desc("id"))
        );

        given(registrationOrganizerQueryService.getMarathonParticipants(
                eq(1L),
                eq(10L),
                eq(101L),
                eq("홍길동"),
                eq(expectedPageable)
        )).willReturn(response);

        mockMvc.perform(get("/api/v1/organizer/marathons/10/registrations")
                        .param("courseId", "101")
                        .param("name", "홍길동")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].registrationId").value(1000))
                .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].phoneNumber").value("010-1111-2222"))
                .andExpect(jsonPath("$.data.content[0].courseId").value(101))
                .andExpect(jsonPath("$.data.content[0].courseType").value("10K"))
                .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.pageRes.page").value(0))
                .andExpect(jsonPath("$.data.pageRes.size").value(10))
                .andExpect(jsonPath("$.data.pageRes.totalElements").value(1))
                .andExpect(jsonPath("$.data.pageRes.totalPages").value(1));

        verify(registrationOrganizerQueryService).getMarathonParticipants(
                1L, 10L, 101L, "홍길동", expectedPageable
        );
    }

    @Test
    @DisplayName("참가자 상세 조회 성공 - 200과 상세 정보가 반환된다")
    void getMarathonParticipantDetail_success() throws Exception {
        setAuthenticatedOrganizer(1L);

        RegistrationParticipantDetailRes response = new RegistrationParticipantDetailRes(
                1000L,
                10L,
                "서울 마라톤",
                101L,
                "10K",
                RegistrationStatus.COMPLETED,
                "홍길동",
                "010-1111-2222",
                "12345",
                "서울시 강남구",
                "101동",
                "L",
                true,
                LocalDateTime.of(2026, 4, 20, 9, 30)
        );

        given(registrationOrganizerQueryService.getMarathonParticipantDetail(1L, 10L, 1000L))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/organizer/marathons/10/registrations/1000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.registrationId").value(1000))
                .andExpect(jsonPath("$.data.marathonId").value(10))
                .andExpect(jsonPath("$.data.marathonTitle").value("서울 마라톤"))
                .andExpect(jsonPath("$.data.courseId").value(101))
                .andExpect(jsonPath("$.data.courseType").value("10K"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.snapName").value("홍길동"))
                .andExpect(jsonPath("$.data.snapPhoneNumber").value("010-1111-2222"))
                .andExpect(jsonPath("$.data.snapZipCode").value("12345"))
                .andExpect(jsonPath("$.data.snapAddress").value("서울시 강남구"))
                .andExpect(jsonPath("$.data.snapDetail").value("101동"))
                .andExpect(jsonPath("$.data.tSize").value("L"))
                .andExpect(jsonPath("$.data.agreedTerms").value(true));

        verify(registrationOrganizerQueryService).getMarathonParticipantDetail(1L, 10L, 1000L);
    }

    @Test
    @DisplayName("참가자 목록 조회 실패 - size가 100 초과이면 400 INVALID_INPUT_VALUE를 반환한다")
    void getMarathonParticipants_fail_invalid_size() throws Exception {
        setAuthenticatedOrganizer(1L);

        mockMvc.perform(get("/api/v1/organizer/marathons/10/registrations")
                        .param("size", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.size").exists());

        verifyNoInteractions(registrationOrganizerQueryService);
    }

    @Test
    @DisplayName("접수 요약 조회 실패 - 권한이 없으면 403 FORBIDDEN을 반환한다")
    void getRegistrationOverview_fail_forbidden() throws Exception {
        setAuthenticatedOrganizer(1L);

        given(registrationOrganizerQueryService.getRegistrationOverview(1L, 10L))
                .willThrow(new CustomException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/v1/organizer/marathons/10/registrations/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }

    private void setAuthenticatedOrganizer(Long organizerId) {
        SecurityUser securityUser = new SecurityUser(
                organizerId,
                "organizer@test.com",
                Role.ORGANIZER,
                List.of(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
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