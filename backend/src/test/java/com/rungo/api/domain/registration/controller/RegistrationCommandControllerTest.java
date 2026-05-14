package com.rungo.api.domain.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rungo.api.domain.registration.dto.CreateRegistrationReq;
import com.rungo.api.domain.registration.dto.CreateRegistrationRes;
import com.rungo.api.domain.registration.service.RegistrationService;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import com.rungo.api.global.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RegistrationController.class)
class RegistrationCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegistrationService registrationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("접수 생성 성공 - 201 상태코드와 공통 응답 규격을 반환하고 사용자 아이디를 서비스에 전달한다")
    void create_success() throws Exception {
        setAuthenticatedUser(1L);

        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", true);
        CreateRegistrationRes response = new CreateRegistrationRes(
                10L,
                20L,
                "서울 마라톤",
                1L,
                "10K",
                "COMPLETED",
                LocalDateTime.of(2026, 4, 16, 9, 0)
        );

        given(registrationService.create(1L, request)).willReturn(response);

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("접수가 완료되었습니다."))
                .andExpect(jsonPath("$.data.registrationId").value(10))
                .andExpect(jsonPath("$.data.marathonId").value(20))
                .andExpect(jsonPath("$.data.marathonTitle").value("서울 마라톤"))
                .andExpect(jsonPath("$.data.courseId").value(1))
                .andExpect(jsonPath("$.data.courseType").value("10K"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(registrationService).create(eq(1L), eq(request));
    }

    @Test
    @DisplayName("접수 생성 실패 - courseId가 없으면 400 INVALID_INPUT_VALUE를 반환한다")
    void create_fail_validation_course_id_null() throws Exception {
        setAuthenticatedUser(1L);

        CreateRegistrationReq request = new CreateRegistrationReq(null, "12345", "서울시 강남구", "101동", "L", true);

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.courseId").exists());

        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("접수 생성 실패 - 우편번호가 비어 있으면 400 INVALID_INPUT_VALUE를 반환한다")
    void create_fail_validation_snap_zip_code_blank() throws Exception {
        setAuthenticatedUser(1L);

        CreateRegistrationReq request = new CreateRegistrationReq(1L, " ", "서울시 강남구", "101동", "L", true);

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.snapZipCode").exists());

        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("접수 생성 실패 - 주소가 비어 있으면 400 INVALID_INPUT_VALUE를 반환한다")
    void create_fail_validation_snap_address_blank() throws Exception {
        setAuthenticatedUser(1L);

        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", " ", "101동", "L", true);

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.snapAddress").exists());

        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("접수 생성 실패 - 티셔츠 사이즈가 비어 있으면 400 INVALID_INPUT_VALUE를 반환한다")
    void create_fail_validation_t_size_blank() throws Exception {
        setAuthenticatedUser(1L);

        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", " ", true);

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.tSize").exists());

        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("접수 생성 실패 - 약관에 동의하지 않으면 400 INVALID_INPUT_VALUE를 반환한다")
    void create_fail_validation_agreed_terms_false() throws Exception {
        setAuthenticatedUser(1L);

        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", false);

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.agreedTerms").exists());

        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("접수 생성 실패 - 정원이 가득 차면 CAPACITY_FULL 예외 응답을 반환한다")
    void create_fail_capacity_full() throws Exception {
        setAuthenticatedUser(1L);

        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", true);
        given(registrationService.create(1L, request))
                .willThrow(new CustomException(ErrorCode.CAPACITY_FULL));

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("CAPACITY_FULL"))
                .andExpect(jsonPath("$.message").value(ErrorCode.CAPACITY_FULL.getMessage()));
    }

    @Test
    @DisplayName("접수 생성 실패 - 모집 중인 대회가 아니면 MARATHON_NOT_OPEN 예외 응답을 반환한다")
    void create_fail_marathon_not_open() throws Exception {
        setAuthenticatedUser(1L);

        CreateRegistrationReq request = new CreateRegistrationReq(1L, "12345", "서울시 강남구", "101동", "L", true);
        given(registrationService.create(1L, request))
                .willThrow(new CustomException(ErrorCode.MARATHON_NOT_OPEN));

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MARATHON_NOT_OPEN"))
                .andExpect(jsonPath("$.message").value(ErrorCode.MARATHON_NOT_OPEN.getMessage()));
    }

    @Test
    @DisplayName("접수 취소 성공 - 200 상태코드와 공통 응답 규격을 반환하고 사용자 아이디와 접수 아이디를 서비스에 전달한다")
    void cancel_success() throws Exception {
        setAuthenticatedUser(1L);

        mockMvc.perform(delete("/api/v1/registrations/{registrationId}", 10L)
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("접수가 취소되었습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verify(registrationService).cancel(1L, 10L);
    }

    @Test
    @DisplayName("접수 취소 실패 - 접수 내역이 없으면 REGISTRATION_NOT_FOUND 예외 응답을 반환한다")
    void cancel_fail_registration_not_found() throws Exception {
        setAuthenticatedUser(1L);

        willThrow(new CustomException(ErrorCode.REGISTRATION_NOT_FOUND))
                .given(registrationService)
                .cancel(1L, 10L);

        mockMvc.perform(delete("/api/v1/registrations/{registrationId}", 10L)
                        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("REGISTRATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(ErrorCode.REGISTRATION_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("접수 취소 실패 - 본인 접수 건이 아니면 FORBIDDEN 예외 응답을 반환한다")
    void cancel_fail_forbidden() throws Exception {
        setAuthenticatedUser(1L);

        willThrow(new CustomException(ErrorCode.FORBIDDEN))
                .given(registrationService)
                .cancel(1L, 10L);

        mockMvc.perform(delete("/api/v1/registrations/{registrationId}", 10L)
                        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(ErrorCode.FORBIDDEN.getMessage()));
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
