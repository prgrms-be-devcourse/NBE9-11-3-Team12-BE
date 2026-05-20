package com.rungo.api.domain.registration.controller

import com.rungo.api.domain.marathon.marathon.dto.PageRes
import com.rungo.api.domain.registration.dto.MyRegistrationRes
import com.rungo.api.domain.registration.enumtype.MyRegistrationStatusFilter
import com.rungo.api.domain.registration.queue.service.RegistrationQueueService
import com.rungo.api.domain.registration.service.RegistrationService
import com.rungo.api.domain.users.enumtype.Role
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
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RegistrationController::class)
class RegistrationReadControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var registrationService: RegistrationService

    @MockitoBean
    private lateinit var registrationQueueService: RegistrationQueueService

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("내 접수 조회 성공 - 기본값으로 ACTIVE, page=0, size=20을 사용해 공통 응답을 반환한다")
    fun getMyRegistrations_active_success_with_default_params() {
        setAuthenticatedUser(1L)

        val response = MyRegistrationRes(
            content = listOf(
                MyRegistrationRes.Item(
                    registrationId = 10L,
                    historyId = null,
                    marathonId = 20L,
                    marathonTitle = "서울 마라톤",
                    courseId = 30L,
                    courseType = "10K",
                    status = "ACTIVE",
                    price = BigDecimal("50000"),
                    eventDate = LocalDate.of(2026, 10, 25),
                    snapName = "홍길동",
                    snapPhoneNumber = "010-1111-2222",
                    snapZipCode = "12345",
                    snapAddress = "서울시 강남구",
                    snapDetail = "101동",
                    tSize = "L",
                    agreedTerms = true,
                    appliedAt = LocalDateTime.of(2026, 4, 20, 9, 30),
                    canceledAt = null
                )
            ),
            pageRes = PageRes(0, 20, 1, 1)
        )

        given(
            registrationService.getMyRegistrations(
                1L,
                MyRegistrationStatusFilter.ACTIVE,
                PageRequest.of(0, 20)
            )
        ).willReturn(response)

        mockMvc.perform(
            get("/api/v1/registrations/me")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
            .andExpect(jsonPath("$.data.content[0].registrationId").value(10))
            .andExpect(jsonPath("$.data.content[0].historyId").value(nullValue()))
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
            .andExpect(jsonPath("$.data.content[0].canceledAt").value(nullValue()))
            .andExpect(jsonPath("$.data.pageRes.page").value(0))
            .andExpect(jsonPath("$.data.pageRes.size").value(20))
            .andExpect(jsonPath("$.data.pageRes.totalElements").value(1))
            .andExpect(jsonPath("$.data.pageRes.totalPages").value(1))

        verify(registrationService).getMyRegistrations(
            1L,
            MyRegistrationStatusFilter.ACTIVE,
            PageRequest.of(0, 20)
        )
    }

    @Test
    @DisplayName("내 접수 조회 성공 - status=CANCELED이면 취소 접수 목록을 반환한다")
    fun getMyRegistrations_canceled_success() {
        setAuthenticatedUser(1L)

        val response = MyRegistrationRes(
            content = listOf(
                MyRegistrationRes.Item(
                    registrationId = 40L,
                    historyId = 10L,
                    marathonId = 20L,
                    marathonTitle = "서울 마라톤",
                    courseId = 30L,
                    courseType = "Half",
                    status = "CANCELED",
                    price = BigDecimal("70000"),
                    eventDate = LocalDate.of(2026, 10, 25),
                    snapName = "홍길동",
                    snapPhoneNumber = "010-1111-2222",
                    snapZipCode = "54321",
                    snapAddress = "서울시 송파구",
                    snapDetail = "202동",
                    tSize = "M",
                    agreedTerms = true,
                    appliedAt = LocalDateTime.of(2026, 4, 1, 8, 0),
                    canceledAt = LocalDateTime.of(2026, 4, 5, 18, 30)
                )
            ),
            pageRes = PageRes(1, 10, 11, 2)
        )

        given(
            registrationService.getMyRegistrations(
                1L,
                MyRegistrationStatusFilter.CANCELED,
                PageRequest.of(1, 10)
            )
        ).willReturn(response)

        mockMvc.perform(
            get("/api/v1/registrations/me")
                .param("status", "CANCELED")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].registrationId").value(40))
            .andExpect(jsonPath("$.data.content[0].historyId").value(10))
            .andExpect(jsonPath("$.data.content[0].status").value("CANCELED"))
            .andExpect(jsonPath("$.data.content[0].canceledAt").value("2026-04-05T18:30:00"))
            .andExpect(jsonPath("$.data.pageRes.page").value(1))
            .andExpect(jsonPath("$.data.pageRes.size").value(10))
            .andExpect(jsonPath("$.data.pageRes.totalElements").value(11))
            .andExpect(jsonPath("$.data.pageRes.totalPages").value(2))

        verify(registrationService).getMyRegistrations(
            1L,
            MyRegistrationStatusFilter.CANCELED,
            PageRequest.of(1, 10)
        )
    }

    @Test
    @DisplayName("내 접수 조회 실패 - page가 0 미만이면 400 INVALID_INPUT_VALUE를 반환한다")
    fun getMyRegistrations_fail_invalid_page() {
        setAuthenticatedUser(1L)

        mockMvc.perform(
            get("/api/v1/registrations/me")
                .param("page", "-1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.data.page").exists())

        verifyNoInteractions(registrationService)
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
}
