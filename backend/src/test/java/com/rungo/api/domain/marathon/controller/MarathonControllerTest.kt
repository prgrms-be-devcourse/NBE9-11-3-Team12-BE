package com.rungo.api.domain.marathon.controller

import com.rungo.api.domain.marathon.course.status.CourseStatus
import com.rungo.api.domain.marathon.marathon.controller.MarathonController
import com.rungo.api.domain.marathon.marathon.dto.CourseItemRes
import com.rungo.api.domain.marathon.marathon.dto.PageRes
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonReq
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonReq.CreateCourseItemReq
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.delete.CancelCourseItemRes
import com.rungo.api.domain.marathon.marathon.dto.delete.CancelMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonDetailRes
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonListRes
import com.rungo.api.domain.marathon.marathon.dto.read.ReadMyMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.read.ReadMyMarathonRes.CourseSummary
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonReq
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonReq.UpdateCourseItemReq
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonRes
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import com.rungo.api.domain.marathon.marathon.enumtype.RecruitmentStatus
import com.rungo.api.domain.marathon.marathon.service.MarathonService
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.security.SecurityUser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(MarathonController::class)
internal class MarathonControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var marathonService: MarathonService

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("마라톤 생성 성공 - 201 상태코드와 공통 응답을 반환한다")
    fun createSuccess() {
        setAuthenticatedOrganizer(1L)

        val response = CreateMarathonRes(
            id = 1L,
            title = "서울 마라톤",
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.of(2026, 10, 3),
            posterImageUrl = "poster.png",
            registrationStartAt = LocalDateTime.of(2026, 8, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            status = MarathonStatus.OPEN,
            courses = listOf(
                CourseItemRes(
                    id = 11L,
                    courseType = "10K",
                    price = BigDecimal.valueOf(30000),
                    capacity = 100,
                    currentCount = 0,
                    remainingCount = 100,
                    status = CourseStatus.AVAILABLE,
                )
            ),
            createdAt = LocalDateTime.of(2026, 7, 1, 12, 0),
        )

        BDDMockito.given(
            marathonService.createMarathon(
                ArgumentMatchers.eq(1L),
                anyCreateMarathonReq(),
            )
        ).willReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/marathons")
                .file(posterImage("poster.png"))
                .param("title", "서울 마라톤")
                .param("region", "서울")
                .param("detailedAddress", "성동구")
                .param("eventDate", "2026-10-03")
                .param("registrationStartAt", "2026-08-01T09:00:00")
                .param("registrationEndAt", "2026-08-10T18:00:00")
                .param("courses[0].courseType", "10K")
                .param("courses[0].price", "30000")
                .param("courses[0].capacity", "100")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(1))

        val requestCaptor = ArgumentCaptor.forClass(CreateMarathonReq::class.java)

        Mockito.verify(marathonService).createMarathon(
            ArgumentMatchers.eq(1L),
            captureCreateMarathonReq(requestCaptor),
        )

        val capturedRequest = requestCaptor.value

        assertEquals("서울 마라톤", capturedRequest.title)
        assertEquals("서울", capturedRequest.region)
        assertEquals("성동구", capturedRequest.detailedAddress)
        assertEquals(LocalDate.of(2026, 10, 3), capturedRequest.eventDate)
        assertEquals("poster.png", capturedRequest.posterImage?.originalFilename)
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), capturedRequest.registrationStartAt)
        assertEquals(LocalDateTime.of(2026, 8, 10, 18, 0), capturedRequest.registrationEndAt)
        assertEquals(1, capturedRequest.courses.size)
        assertEquals("10K", capturedRequest.courses[0].courseType)
        assertEquals(BigDecimal.valueOf(30000), capturedRequest.courses[0].price)
        assertEquals(100, capturedRequest.courses[0].capacity)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - title이 없으면 400 반환")
    fun createFailTitleNull() {
        setAuthenticatedOrganizer(1L)

        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/marathons")
                .file(posterImage("poster.png"))
                .param("region", "서울")
                .param("detailedAddress", "성동구")
                .param("eventDate", "2026-10-03")
                .param("registrationStartAt", "2026-08-01T09:00:00")
                .param("registrationEndAt", "2026-08-10T18:00:00")
                .param("courses[0].courseType", "10K")
                .param("courses[0].price", "30000")
                .param("courses[0].capacity", "100")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))

        Mockito.verifyNoInteractions(marathonService)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - courses가 비어있으면 400 반환")
    fun createFailCoursesEmpty() {
        setAuthenticatedOrganizer(1L)

        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/marathons")
                .file(posterImage("poster.png"))
                .param("title", "서울 마라톤")
                .param("region", "서울")
                .param("detailedAddress", "성동구")
                .param("eventDate", "2026-10-03")
                .param("registrationStartAt", "2026-08-01T09:00:00")
                .param("registrationEndAt", "2026-08-10T18:00:00")
        )
            .andExpect(status().isBadRequest())

        Mockito.verifyNoInteractions(marathonService)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - courseType이 없으면 400 반환")
    fun createFailCourseTypeNull() {
        setAuthenticatedOrganizer(1L)

        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/marathons")
                .file(posterImage("poster.png"))
                .param("title", "서울 마라톤")
                .param("region", "서울")
                .param("detailedAddress", "성동구")
                .param("eventDate", "2026-10-03")
                .param("registrationStartAt", "2026-08-01T09:00:00")
                .param("registrationEndAt", "2026-08-10T18:00:00")
                .param("courses[0].price", "30000")
                .param("courses[0].capacity", "100")
        )
            .andExpect(status().isBadRequest())

        Mockito.verifyNoInteractions(marathonService)
    }

    @Test
    @DisplayName("마라톤 생성 실패 - courseType에 공백이 들어오면 400 반환")
    fun createFailCourseTypeBlank() {
        setAuthenticatedOrganizer(1L)

        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/marathons")
                .file(posterImage("poster.png"))
                .param("title", "서울 마라톤")
                .param("region", "서울")
                .param("detailedAddress", "성동구")
                .param("eventDate", "2026-10-03")
                .param("registrationStartAt", "2026-08-01T09:00:00")
                .param("registrationEndAt", "2026-08-10T18:00:00")
                .param("courses[0].courseType", "   ")
                .param("courses[0].price", "30000")
                .param("courses[0].capacity", "100")
        )
            .andExpect(status().isBadRequest())

        Mockito.verifyNoInteractions(marathonService)
    }

    @Test
    @DisplayName("마라톤 목록 조회 성공 - 200 상태코드와 공통 응답 규격을 반환한다")
    fun getMarathonsSuccess() {
        val response = MarathonListRes(
            content = listOf(
                MarathonListRes.Item(
                    id = 1L,
                    title = "서울 마라톤",
                    region = "서울",
                    detailedAddress = "성동구",
                    eventDate = LocalDate.of(2026, 10, 3),
                    posterImageUrl = "poster1.png",
                    registrationStartAt = LocalDateTime.of(2026, 8, 1, 9, 0),
                    registrationEndAt = LocalDateTime.of(2026, 8, 31, 18, 0),
                    status = MarathonStatus.OPEN,
                    totalCapacity = 100,
                    totalCurrentCount = 10,
                    recruitmentStatus = RecruitmentStatus.OPEN,
                ),
                MarathonListRes.Item(
                    id = 2L,
                    title = "부산 마라톤",
                    region = "부산",
                    detailedAddress = "해운대구",
                    eventDate = LocalDate.of(2026, 11, 1),
                    posterImageUrl = "poster2.png",
                    registrationStartAt = LocalDateTime.of(2026, 9, 1, 9, 0),
                    registrationEndAt = LocalDateTime.of(2026, 9, 30, 18, 0),
                    status = MarathonStatus.OPEN,
                    totalCapacity = 200,
                    totalCurrentCount = 20,
                    recruitmentStatus = RecruitmentStatus.OPEN,
                ),
            ),
            pageRes = PageRes(0, 20, 2L, 1),
        )

        BDDMockito.given(
            marathonService.getMarathons(anyPageable())
        ).willReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/marathons")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].id").value(1))
            .andExpect(jsonPath("$.data.content[0].title").value("서울 마라톤"))
            .andExpect(jsonPath("$.data.content[1].id").value(2))
            .andExpect(jsonPath("$.data.content[1].title").value("부산 마라톤"))
            .andExpect(jsonPath("$.data.pageRes.page").value(0))
            .andExpect(jsonPath("$.data.pageRes.size").value(20))
            .andExpect(jsonPath("$.data.pageRes.totalElements").value(2))
            .andExpect(jsonPath("$.data.pageRes.totalPages").value(1))
    }

    @Test
    @DisplayName("마라톤 상세 조회 성공 - 200 상태코드와 공통 응답 규격을 반환한다")
    fun getMarathonDetailSuccess() {
        val response = MarathonDetailRes(
            id = 1L,
            title = "서울 마라톤",
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.of(2026, 10, 3),
            posterImageUrl = "poster.png",
            registrationStartAt = LocalDateTime.of(2026, 8, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 8, 31, 18, 0),
            status = MarathonStatus.OPEN,
            recruitmentStatus = RecruitmentStatus.OPEN,
            courses = listOf(
                CourseItemRes(
                    id = 11L,
                    courseType = "10K",
                    price = BigDecimal.valueOf(30000),
                    capacity = 100,
                    currentCount = 10,
                    remainingCount = 90,
                    status = CourseStatus.AVAILABLE,
                ),
                CourseItemRes(
                    id = 12L,
                    courseType = "21K",
                    price = BigDecimal.valueOf(50000),
                    capacity = 50,
                    currentCount = 5,
                    remainingCount = 45,
                    status = CourseStatus.AVAILABLE,
                ),
            ),
            createdAt = LocalDateTime.of(2026, 7, 1, 12, 0),
        )

        BDDMockito.given(marathonService.getMarathonDetail(1L))
            .willReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/marathons/{marathonId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.title").value("서울 마라톤"))
            .andExpect(jsonPath("$.data.region").value("서울"))
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.courses.length()").value(2))
            .andExpect(jsonPath("$.data.courses[0].id").value(11))
            .andExpect(jsonPath("$.data.courses[0].courseType").value("10K"))
            .andExpect(jsonPath("$.data.courses[0].price").value(30000))
            .andExpect(jsonPath("$.data.courses[0].remainingCount").value(90))
    }

    @Test
    @DisplayName("마라톤 상세 조회 실패 - 존재하지 않는 대회면 MARATHON_NOT_FOUND 예외 응답을 반환한다")
    fun getMarathonDetailFailNotFound() {
        BDDMockito.given(marathonService.getMarathonDetail(999L))
            .willThrow(CustomException(ErrorCode.MARATHON_NOT_FOUND))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/marathons/{marathonId}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("MARATHON_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value(ErrorCode.MARATHON_NOT_FOUND.message))
    }

    @Test
    @DisplayName("마라톤 취소 성공 - 200 상태코드와 공통 응답 규격을 반환한다")
    fun cancelMarathonSuccess() {
        setAuthenticatedOrganizer(1L)

        val response = CancelMarathonRes(
            marathonId = 10L,
            title = "서울 마라톤",
            eventDate = LocalDate.of(2026, 10, 3),
            status = MarathonStatus.CANCELING,
            courses = listOf(
                CancelCourseItemRes(101L, "5K"),
                CancelCourseItemRes(102L, "10K"),
            ),
        )

        BDDMockito.given(
            marathonService.cancelMarathon(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(10L),
            )
        ).willReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/marathons/{id}/cancel", 10L)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
            .andExpect(jsonPath("$.data.marathonId").value(10))
            .andExpect(jsonPath("$.data.title").value("서울 마라톤"))
            .andExpect(jsonPath("$.data.eventDate").value("2026-10-03"))
            .andExpect(jsonPath("$.data.status").value("CANCELING"))
            .andExpect(jsonPath("$.data.courses.length()").value(2))
            .andExpect(jsonPath("$.data.courses[0].courseType").value("5K"))
            .andExpect(jsonPath("$.data.courses[1].courseType").value("10K"))

        Mockito.verify(marathonService).cancelMarathon(1L, 10L)
    }

    @Test
    @DisplayName("마라톤 수정 성공 - 201 상태코드와 공통 응답 규격을 반환한다")
    fun updateMarathonSuccess() {
        setAuthenticatedOrganizer(1L)

        val response = UpdateMarathonRes(
            id = 10L,
            title = "수정된 서울 마라톤",
            region = "부산",
            detailedAddress = "중구",
            eventDate = LocalDate.of(2026, 11, 15),
            posterImageUrl = "updated-poster.png",
            registrationStartAt = LocalDateTime.of(2026, 9, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 9, 30, 18, 0),
            status = MarathonStatus.OPEN,
            courses = listOf(
                CourseItemRes(
                    id = 101L,
                    courseType = "5K",
                    price = BigDecimal.valueOf(35000),
                    capacity = 120,
                    currentCount = 10,
                    remainingCount = 110,
                    status = CourseStatus.AVAILABLE,
                ),
                CourseItemRes(
                    id = 102L,
                    courseType = "10K",
                    price = BigDecimal.valueOf(55000),
                    capacity = 220,
                    currentCount = 20,
                    remainingCount = 200,
                    status = CourseStatus.AVAILABLE,
                ),
            ),
            LocalDateTime.of(2026, 8, 1, 12, 0),
        )

        BDDMockito.given(
            marathonService.updateMarathon(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(10L),
                anyUpdateMarathonReq(),
            )
        ).willReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.multipart(HttpMethod.PATCH, "/api/v1/marathons/{marathonId}", 10L)
                .file(posterImage("updated-poster.png"))
                .param("title", "수정된 서울 마라톤")
                .param("region", "부산")
                .param("detailedAddress", "중구")
                .param("eventDate", "2026-11-15")
                .param("registrationStartAt", "2026-09-01T09:00:00")
                .param("registrationEndAt", "2026-09-30T18:00:00")
                .param("courses[0].id", "101")
                .param("courses[0].courseType", "5K")
                .param("courses[0].price", "35000")
                .param("courses[0].capacity", "120")
                .param("courses[1].id", "102")
                .param("courses[1].courseType", "10K")
                .param("courses[1].price", "55000")
                .param("courses[1].capacity", "220")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("마라톤 대회 수정 성공"))
            .andExpect(jsonPath("$.data.id").value(10))
            .andExpect(jsonPath("$.data.title").value("수정된 서울 마라톤"))
            .andExpect(jsonPath("$.data.region").value("부산"))
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.courses.length()").value(2))
            .andExpect(jsonPath("$.data.courses[0].id").value(101))
            .andExpect(jsonPath("$.data.courses[0].courseType").value("5K"))
            .andExpect(jsonPath("$.data.courses[0].price").value(35000))
            .andExpect(jsonPath("$.data.courses[0].capacity").value(120))
            .andExpect(jsonPath("$.data.courses[1].id").value(102))
            .andExpect(jsonPath("$.data.courses[1].courseType").value("10K"))

        val requestCaptor = ArgumentCaptor.forClass(UpdateMarathonReq::class.java)

        Mockito.verify(marathonService).updateMarathon(
            ArgumentMatchers.eq(1L),
            ArgumentMatchers.eq(10L),
            captureUpdateMarathonReq(requestCaptor),
        )

        val capturedRequest = requestCaptor.value
        val courses = capturedRequest.courses.orEmpty()

        assertEquals("수정된 서울 마라톤", capturedRequest.title)
        assertEquals("부산", capturedRequest.region)
        assertEquals("중구", capturedRequest.detailedAddress)
        assertEquals(LocalDate.of(2026, 11, 15), capturedRequest.eventDate)
        assertEquals("updated-poster.png", capturedRequest.posterImage?.originalFilename)
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 0), capturedRequest.registrationStartAt)
        assertEquals(LocalDateTime.of(2026, 9, 30, 18, 0), capturedRequest.registrationEndAt)
        assertEquals(2, courses.size)
        assertEquals(101L, courses[0].id)
        assertEquals("5K", courses[0].courseType)
        assertEquals(BigDecimal.valueOf(35000), courses[0].price)
        assertEquals(120, courses[0].capacity)
        assertEquals(102L, courses[1].id)
        assertEquals("10K", courses[1].courseType)
        assertEquals(BigDecimal.valueOf(55000), courses[1].price)
        assertEquals(220, courses[1].capacity)
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 코스 ID가 null이면 400 반환")
    fun updateMarathonFailCourseIdNull() {
        setAuthenticatedOrganizer(1L)

        mockMvc.perform(
            MockMvcRequestBuilders.multipart(HttpMethod.PATCH, "/api/v1/marathons/{marathonId}", 10L)
                .file(posterImage("updated-poster.png"))
                .param("title", "수정된 서울 마라톤")
                .param("region", "부산")
                .param("detailedAddress", "중구")
                .param("eventDate", "2026-11-15")
                .param("registrationStartAt", "2026-09-01T09:00:00")
                .param("registrationEndAt", "2026-09-30T18:00:00")
                .param("courses[0].courseType", "5K")
                .param("courses[0].price", "35000")
                .param("courses[0].capacity", "120")
        )
            .andExpect(status().isBadRequest())
    }

    @Test
    @DisplayName("주최자 내 대회 조회 성공 - 200 상태코드와 공통 응답 규격을 반환한다")
    fun getMyMarathonsSuccess() {
        setAuthenticatedOrganizer(1L)

        val response1 = ReadMyMarathonRes(
            id = 10L,
            title = "서울 마라톤",
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.of(2026, 10, 3),
            posterImageUrl = "poster.png",
            registrationStartAt = LocalDateTime.of(2026, 8, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 8, 31, 18, 0),
            status = MarathonStatus.OPEN,
            courses = listOf(
                CourseSummary(101L, "5K", BigDecimal.valueOf(30000), 100, 0),
                CourseSummary(102L, "10K", BigDecimal.valueOf(50000), 200, 0),
            ),
        )

        val response2 = ReadMyMarathonRes(
            id = 11L,
            title = "부산 마라톤",
            region = "부산",
            detailedAddress = "해운대구",
            eventDate = LocalDate.of(2026, 11, 1),
            posterImageUrl = "poster2.png",
            registrationStartAt = LocalDateTime.of(2026, 9, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 9, 30, 18, 0),
            status = MarathonStatus.CANCELING,
            courses = listOf(
                CourseSummary(201L, "HALF", BigDecimal.valueOf(40000), 150, 10),
            ),
        )

        BDDMockito.given(marathonService.getMyMarathons(1L))
            .willReturn(listOf(response1, response2))

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/marathons/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(10))
            .andExpect(jsonPath("$.data[0].title").value("서울 마라톤"))
            .andExpect(jsonPath("$.data[0].region").value("서울"))
            .andExpect(jsonPath("$.data[0].status").value("OPEN"))
            .andExpect(jsonPath("$.data[0].courses.length()").value(2))
            .andExpect(jsonPath("$.data[0].courses[0].courseId").value(101))
            .andExpect(jsonPath("$.data[0].courses[0].courseType").value("5K"))
            .andExpect(jsonPath("$.data[1].id").value(11))
            .andExpect(jsonPath("$.data[1].title").value("부산 마라톤"))
            .andExpect(jsonPath("$.data[1].status").value("CANCELING"))

        Mockito.verify(marathonService).getMyMarathons(1L)
    }

    private fun setAuthenticatedOrganizer(userId: Long) {
        val securityUser = SecurityUser(
            id = userId,
            email = "organizer@test.com",
            role = Role.ORGANIZER,
            authorities = listOf(SimpleGrantedAuthority("ROLE_ORGANIZER")),
        )

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                securityUser,
                null,
                securityUser.authorities,
            )
    }

    private fun posterImage(originalFilename: String): MockMultipartFile =
        MockMultipartFile(
            "posterImage",
            originalFilename,
            "image/png",
            "poster".toByteArray(),
        )

    private fun anyPageable(): Pageable {
        ArgumentMatchers.any(Pageable::class.java)
        return PageRequest.of(0, 20)
    }

    private fun anyCreateMarathonReq(): CreateMarathonReq {
        ArgumentMatchers.any(CreateMarathonReq::class.java)
        return createMarathonReq()
    }

    private fun anyUpdateMarathonReq(): UpdateMarathonReq {
        ArgumentMatchers.any(UpdateMarathonReq::class.java)
        return updateMarathonReq()
    }

    private fun captureCreateMarathonReq(
        captor: ArgumentCaptor<CreateMarathonReq>,
    ): CreateMarathonReq {
        captor.capture()
        return createMarathonReq()
    }

    private fun captureUpdateMarathonReq(
        captor: ArgumentCaptor<UpdateMarathonReq>,
    ): UpdateMarathonReq {
        captor.capture()
        return updateMarathonReq()
    }

    private fun createMarathonReq(): CreateMarathonReq =
        CreateMarathonReq(
            title = "서울 마라톤",
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.of(2026, 10, 3),
            posterImage = posterImage("poster.png"),
            registrationStartAt = LocalDateTime.of(2026, 8, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            courses = listOf(
                CreateCourseItemReq(
                    "10K",
                    BigDecimal.valueOf(30000),
                    100,
                )
            ),
        )

    private fun updateMarathonReq(): UpdateMarathonReq =
        UpdateMarathonReq(
            title = "수정된 서울 마라톤",
            region = "부산",
            detailedAddress = "중구",
            eventDate = LocalDate.of(2026, 11, 15),
            posterImage = posterImage("updated-poster.png"),
            registrationStartAt = LocalDateTime.of(2026, 9, 1, 9, 0),
            registrationEndAt = LocalDateTime.of(2026, 9, 30, 18, 0),
            courses = listOf(
                UpdateCourseItemReq(
                    101L,
                    "5K",
                    BigDecimal.valueOf(35000),
                    120,
                )
            ),
        )
}
