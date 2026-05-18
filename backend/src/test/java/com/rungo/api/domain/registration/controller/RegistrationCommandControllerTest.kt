package com.rungo.api.domain.registration.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.dto.CreateRegistrationRes
import com.rungo.api.domain.registration.service.RegistrationService
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.security.SecurityUser
import org.hamcrest.Matchers.nullValue
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RegistrationController::class)
class RegistrationCommandControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var registrationService: RegistrationService

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("접수 생성 성공 - 201 상태코드와 공통 응답 규격을 반환하고 사용자 아이디를 서비스에 전달한다")
    fun create_success() {
        setAuthenticatedUser(1L)

        val request = createRegistrationRequest()
        val response = CreateRegistrationRes(
            10L,
            20L,
            "서울 마라톤",
            1L,
            "10K",
            "COMPLETED",
            LocalDateTime.of(2026, 4, 16, 9, 0)
        )

        given(registrationService.create(1L, request)).willReturn(response)

        performCreate(request)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("접수가 완료되었습니다."))
            .andExpect(jsonPath("$.data.registrationId").value(10))
            .andExpect(jsonPath("$.data.marathonId").value(20))
            .andExpect(jsonPath("$.data.marathonTitle").value("서울 마라톤"))
            .andExpect(jsonPath("$.data.courseId").value(1))
            .andExpect(jsonPath("$.data.courseType").value("10K"))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))

        verify(registrationService).create(1L, request)
    }

    @Test
    @DisplayName("접수 생성 실패 - courseId가 null이면 400 INVALID_INPUT_VALUE를 반환한다")
    fun create_fail_validation_course_id_zero() {
        setAuthenticatedUser(1L)

        performCreate(
            """
            {
              "courseId": null,
              "snapZipCode": "12345",
              "snapAddress": "서울시 강남구",
              "snapDetail": "101동",
              "tSize": "L",
              "agreedTerms": true
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.data.courseId").exists())

        verifyNoInteractions(registrationService)
    }

    @Test
    @DisplayName("접수 생성 실패 - 우편번호가 비어 있으면 400 INVALID_INPUT_VALUE를 반환한다")
    fun create_fail_validation_snap_zip_code_blank() {
        setAuthenticatedUser(1L)

        performCreate(
            """
            {
              "courseId": 1,
              "snapZipCode": " ",
              "snapAddress": "서울시 강남구",
              "snapDetail": "101동",
              "tSize": "L",
              "agreedTerms": true
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.data.snapZipCode").exists())

        verifyNoInteractions(registrationService)
    }

    @Test
    @DisplayName("접수 생성 실패 - 주소가 비어 있으면 400 INVALID_INPUT_VALUE를 반환한다")
    fun create_fail_validation_snap_address_blank() {
        setAuthenticatedUser(1L)

        performCreate(
            """
            {
              "courseId": 1,
              "snapZipCode": "12345",
              "snapAddress": " ",
              "snapDetail": "101동",
              "tSize": "L",
              "agreedTerms": true
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.data.snapAddress").exists())

        verifyNoInteractions(registrationService)
    }

    @Test
    @DisplayName("접수 생성 실패 - 티셔츠 사이즈가 비어 있으면 400 INVALID_INPUT_VALUE를 반환한다")
    fun create_fail_validation_t_size_blank() {
        setAuthenticatedUser(1L)

        performCreate(
            """
            {
              "courseId": 1,
              "snapZipCode": "12345",
              "snapAddress": "서울시 강남구",
              "snapDetail": "101동",
              "tSize": " ",
              "agreedTerms": true
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.data.tSize").exists())

        verifyNoInteractions(registrationService)
    }
    @Test
    @DisplayName("접수 생성 실패 - 티셔츠 사이즈가 null이면 400 INVALID_INPUT_VALUE를 반환한다")
    fun create_fail_validation_t_size_null() {
        setAuthenticatedUser(1L)

        performCreate(
            """
            {
              "courseId": 1,
              "snapZipCode": "12345",
              "snapAddress": "서울시 강남구",
              "snapDetail": "101동",
              "tSize": null,
              "agreedTerms": true
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.message").value("필수 입력값이 누락되었습니다."))

        verifyNoInteractions(registrationService)
    }

    @Test
    @DisplayName("접수 생성 실패 - 약관에 동의하지 않으면 400 INVALID_INPUT_VALUE를 반환한다")
    fun create_fail_validation_agreed_terms_false() {
        setAuthenticatedUser(1L)

        performCreate(
            """
            {
              "courseId": 1,
              "snapZipCode": "12345",
              "snapAddress": "서울시 강남구",
              "snapDetail": "101동",
              "tSize": "L",
              "agreedTerms": false
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.data.agreedTerms").exists())

        verifyNoInteractions(registrationService)
    }

    @Test
    @DisplayName("접수 생성 실패 - 정원이 가득 차면 CAPACITY_FULL 예외 응답을 반환한다")
    fun create_fail_capacity_full() {
        setAuthenticatedUser(1L)

        val request = createRegistrationRequest()
        given(registrationService.create(1L, request))
            .willThrow(CustomException(ErrorCode.CAPACITY_FULL))

        performCreate(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("CAPACITY_FULL"))
            .andExpect(jsonPath("$.message").value(ErrorCode.CAPACITY_FULL.message))
    }

    @Test
    @DisplayName("접수 생성 실패 - 모집 중인 대회가 아니면 MARATHON_NOT_OPEN 예외 응답을 반환한다")
    fun create_fail_marathon_not_open() {
        setAuthenticatedUser(1L)

        val request = createRegistrationRequest()
        given(registrationService.create(1L, request))
            .willThrow(CustomException(ErrorCode.MARATHON_NOT_OPEN))

        performCreate(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("MARATHON_NOT_OPEN"))
            .andExpect(jsonPath("$.message").value(ErrorCode.MARATHON_NOT_OPEN.message))
    }

    @Test
    @DisplayName("접수 취소 성공 - 200 상태코드와 공통 응답 규격을 반환하고 사용자 아이디와 접수 아이디를 서비스에 전달한다")
    fun cancel_success() {
        setAuthenticatedUser(1L)

        mockMvc.perform(delete("/api/v1/registrations/{registrationId}", 10L))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("접수가 취소되었습니다."))
            .andExpect(jsonPath("$.data").value(nullValue()))

        verify(registrationService).cancel(1L, 10L)
    }

    @Test
    @DisplayName("접수 취소 실패 - 접수 내역이 없으면 REGISTRATION_NOT_FOUND 예외 응답을 반환한다")
    fun cancel_fail_registration_not_found() {
        setAuthenticatedUser(1L)

        given(registrationService.cancel(1L, 10L))
            .willThrow(CustomException(ErrorCode.REGISTRATION_NOT_FOUND))

        mockMvc.perform(delete("/api/v1/registrations/{registrationId}", 10L))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("REGISTRATION_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value(ErrorCode.REGISTRATION_NOT_FOUND.message))
    }

    @Test
    @DisplayName("접수 취소 실패 - 본인 접수 건이 아니면 FORBIDDEN 예외 응답을 반환한다")
    fun cancel_fail_forbidden() {
        setAuthenticatedUser(1L)

        given(registrationService.cancel(1L, 10L))
            .willThrow(CustomException(ErrorCode.FORBIDDEN))

        mockMvc.perform(delete("/api/v1/registrations/{registrationId}", 10L))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value(ErrorCode.FORBIDDEN.message))
    }

    private fun performCreate(request: CreateRegistrationReq) =
        mockMvc.perform(
            post("/api/v1/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )

    private fun performCreate(requestJson: String) =
        mockMvc.perform(
            post("/api/v1/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )

    private fun createRegistrationRequest(
        courseId: Long = 1L,
        snapZipCode: String = "12345",
        snapAddress: String = "서울시 강남구",
        snapDetail: String? = "101동",
        tSize: String = "L",
        agreedTerms: Boolean = true,
    ): CreateRegistrationReq =
        CreateRegistrationReq(
            courseId = courseId,
            snapZipCode = snapZipCode,
            snapAddress = snapAddress,
            snapDetail = snapDetail,
            tSize = tSize,
            agreedTerms = agreedTerms
        )

    private fun setAuthenticatedUser(userId: Long) {
        val securityUser = SecurityUser(
            userId,
            "test@test.com",
            Role.PARTICIPANT,
            listOf(SimpleGrantedAuthority("ROLE_PARTICIPANT"))
        )

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                securityUser,
                null,
                securityUser.authorities
            )
    }
}
