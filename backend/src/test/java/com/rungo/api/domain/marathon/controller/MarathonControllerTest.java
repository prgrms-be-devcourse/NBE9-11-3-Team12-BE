package com.rungo.api.domain.marathon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rungo.api.domain.marathon.course.status.CourseStatus;
import com.rungo.api.domain.marathon.marathon.controller.MarathonController;
import com.rungo.api.domain.marathon.marathon.dto.CourseItemRes;
import com.rungo.api.domain.marathon.marathon.dto.PageRes;
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonReq;
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonRes;
import com.rungo.api.domain.marathon.marathon.dto.delete.CancelCourseItemRes;
import com.rungo.api.domain.marathon.marathon.dto.delete.CancelMarathonRes;
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonDetailRes;
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonListRes;
import com.rungo.api.domain.marathon.marathon.dto.read.ReadMyMarathonRes;
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonReq;
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonRes;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.enumtype.RecruitmentStatus;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.marathon.marathon.service.MarathonService;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import com.rungo.api.global.security.SecurityUser;
import com.rungo.api.domain.users.enumtype.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(MarathonController.class)
class MarathonControllerTest {

    @Autowired

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean

    private MarathonService marathonService;

    @MockitoBean
    private MarathonRepository marathonRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityUser user = new SecurityUser(
                userId,
                "test@test.com",
                Role.ORGANIZER,
                List.of(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("마라톤 생성 성공 - 201 상태코드와 공통 응답을 반환한다")
    void create_success() throws Exception {

        setAuthenticatedUser(1L);

        CreateMarathonReq req = new CreateMarathonReq(
                "서울 마라톤",
                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),
                posterImage("poster.png"),
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 10, 18, 0),
                List.of(
                        new CreateMarathonReq.CreateCourseItemReq(
                                "10K",
                                BigDecimal.valueOf(30000),
                                100
                        )
                )
        );

        CreateMarathonRes res = new CreateMarathonRes(
                1L,
                "서울 마라톤",
                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),
                "poster.png",
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 10, 18, 0),
                MarathonStatus.OPEN,
                List.of(
                        new CourseItemRes(
                                11L,
                                "10K",
                                BigDecimal.valueOf(30000),
                                100,
                                0,
                                100,
                                CourseStatus.AVAILABLE
                        )
                ),
                LocalDateTime.of(2026, 7, 1, 12, 0)
        );

        given(marathonService.createMarathon(eq(1L), any(CreateMarathonReq.class))).willReturn(res);

        mockMvc.perform(multipart("/api/v1/marathons")
                        .file(posterImage("poster.png"))
                        .param("title", "서울 마라톤")
                        .param("region", "서울")
                        .param("detailedAddress", "성동구")
                        .param("eventDate", "2026-10-03")
                        .param("registrationStartAt", "2026-08-01T09:00:00")
                        .param("registrationEndAt", "2026-08-10T18:00:00")
                        .param("courses[0].courseType", "10K")
                        .param("courses[0].price", "30000")
                        .param("courses[0].capacity", "100"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1));

        ArgumentCaptor<CreateMarathonReq> requestCaptor = ArgumentCaptor.forClass(CreateMarathonReq.class);
        verify(marathonService).createMarathon(eq(1L), requestCaptor.capture());

        CreateMarathonReq capturedRequest = requestCaptor.getValue();
        assertEquals("서울 마라톤", capturedRequest.title());
        assertEquals("서울", capturedRequest.region());
        assertEquals("성동구", capturedRequest.detailedAddress());
        assertEquals(LocalDate.of(2026, 10, 3), capturedRequest.eventDate());
        assertEquals("poster.png", capturedRequest.posterImage().getOriginalFilename());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), capturedRequest.registrationStartAt());
        assertEquals(LocalDateTime.of(2026, 8, 10, 18, 0), capturedRequest.registrationEndAt());
        assertEquals(1, capturedRequest.courses().size());
        assertEquals("10K", capturedRequest.courses().get(0).courseType());
        assertEquals(BigDecimal.valueOf(30000), capturedRequest.courses().get(0).price());
        assertEquals(100, capturedRequest.courses().get(0).capacity());
    }

    @Test
    @DisplayName("마라톤 생성 실패 - title이 없으면 400 반환")
    void create_fail_title_null() throws Exception {

        setAuthenticatedUser(1L);

        String request = """
            {
              "region": "서울",
              "eventDate": "2026-10-03",
              "registrationStartAt": "2026-08-01T09:00:00",
              "registrationEndAt": "2026-08-10T18:00:00",
              "courses": [
                {
                  "courseType": "10K",
                  "price": 30000,
                  "capacity": 100
                }
              ]
            }
            """;

        mockMvc.perform(multipart("/api/v1/marathons")
                        .file(posterImage("poster.png"))
                        .param("region", "서울")
                        .param("detailedAddress", "성동구")
                        .param("eventDate", "2026-10-03")
                        .param("registrationStartAt", "2026-08-01T09:00:00")
                        .param("registrationEndAt", "2026-08-10T18:00:00")
                        .param("courses[0].courseType", "10K")
                        .param("courses[0].price", "30000")
                        .param("courses[0].capacity", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        verifyNoInteractions(marathonService);
    }

    @Test
    @DisplayName("마라톤 생성 실패 - courses가 비어있으면 400 반환")
    void create_fail_courses_empty() throws Exception {

        setAuthenticatedUser(1L);

        String request = """
            {
              "title": "서울 마라톤",
              "region": "서울",
              "eventDate": "2026-10-03",
              "registrationStartAt": "2026-08-01T09:00:00",
              "registrationEndAt": "2026-08-10T18:00:00",
              "courses": []
            }
            """;

        mockMvc.perform(multipart("/api/v1/marathons")
                        .file(posterImage("poster.png"))
                        .param("title", "서울 마라톤")
                        .param("region", "서울")
                        .param("detailedAddress", "성동구")
                        .param("eventDate", "2026-10-03")
                        .param("registrationStartAt", "2026-08-01T09:00:00")
                        .param("registrationEndAt", "2026-08-10T18:00:00"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(marathonService);
    }

    @Test
    @DisplayName("마라톤 생성 실패 - courseType이 비어있으면 400 반환")
    void create_fail_courseType_Null() throws Exception {

        setAuthenticatedUser(1L);

        String request = """
            {
              "title": "서울 마라톤",
              "region": "서울",
              "eventDate": "2026-10-03",
              "registrationStartAt": "2026-08-01T09:00:00",
              "registrationEndAt": "2026-08-10T18:00:00",
              "courses": [
                {
                  
                  "price": 30000,
                  "capacity": 100
                }
              ]
            }
            """;

        mockMvc.perform(multipart("/api/v1/marathons")
                        .file(posterImage("poster.png"))
                        .param("title", "서울 마라톤")
                        .param("region", "서울")
                        .param("detailedAddress", "성동구")
                        .param("eventDate", "2026-10-03")
                        .param("registrationStartAt", "2026-08-01T09:00:00")
                        .param("registrationEndAt", "2026-08-10T18:00:00")
                        .param("courses[0].price", "30000")
                        .param("courses[0].capacity", "100"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(marathonService);
    }

    @Test
    @DisplayName("마라톤 생성 실패 - courseType의 문자열에 빈 공백들어 올 시 400 반환")
    void create_fail_courseType_blank() throws Exception {

        setAuthenticatedUser(1L);

        String request = """
            {
              "title": "서울 마라톤",
              "region": "서울",
              "eventDate": "2026-10-03",
              "registrationStartAt": "2026-08-01T09:00:00",
              "registrationEndAt": "2026-08-10T18:00:00",
              "courses": [
                {
                  
                  "courseType": "   ",
                  "price": 30000,
                  "capacity": 100
                }
              ]
            }
            """;

        mockMvc.perform(multipart("/api/v1/marathons")
                        .file(posterImage("poster.png"))
                        .param("title", "서울 마라톤")
                        .param("region", "서울")
                        .param("detailedAddress", "성동구")
                        .param("eventDate", "2026-10-03")
                        .param("registrationStartAt", "2026-08-01T09:00:00")
                        .param("registrationEndAt", "2026-08-10T18:00:00")
                        .param("courses[0].courseType", "   ")
                        .param("courses[0].price", "30000")
                        .param("courses[0].capacity", "100"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(marathonService);
    }

    @Test
    @DisplayName("마라톤 목록 조회 성공 - 200 상태코드와 공통 응답 규격을 반환한다")
    void get_marathons_success() throws Exception {

        MarathonListRes.Item item1 = new MarathonListRes.Item(
                1L,
                "서울 마라톤",
                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),
                "poster1.png",
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 31, 18, 0),
                MarathonStatus.OPEN,
                100,
                10,
                RecruitmentStatus.OPEN
        );

        MarathonListRes.Item item2 = new MarathonListRes.Item(
                2L,
                "부산 마라톤",
                "부산",
                "해운대구",
                LocalDate.of(2026, 11, 1),
                "poster2.png",
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 30, 18, 0),
                MarathonStatus.OPEN,
                200,
                20,
                RecruitmentStatus.OPEN
        );

        MarathonListRes response = new MarathonListRes(
                List.of(item1, item2),
                new PageRes(0, 20, 2L, 1)
        );

        given(marathonService.getMarathons(org.mockito.ArgumentMatchers.any()))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/marathons")
                        .contentType(MediaType.APPLICATION_JSON))
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
                .andExpect(jsonPath("$.data.pageRes.totalPages").value(1));
    }

    @Test
    @DisplayName("마라톤 상세 조회 성공 - 200 상태코드와 공통 응답 규격을 반환한다")
    void get_marathon_detail_success() throws Exception {

        MarathonDetailRes response = new MarathonDetailRes(

                1L,

                "서울 마라톤",

                "서울",

                "성동구",

                LocalDate.of(2026, 10, 3),

                "poster.png",

                LocalDateTime.of(2026, 8, 1, 9, 0),

                LocalDateTime.of(2026, 8, 31, 18, 0),

                MarathonStatus.OPEN,

                RecruitmentStatus.OPEN,

                List.of(

                        new com.rungo.api.domain.marathon.marathon.dto.CourseItemRes(

                                11L,

                                "10K",

                                BigDecimal.valueOf(30000),

                                100,

                                10,

                                90,

                                CourseStatus.AVAILABLE

                        ),

                        new com.rungo.api.domain.marathon.marathon.dto.CourseItemRes(

                                12L,

                                "21K",

                                BigDecimal.valueOf(50000),

                                50,

                                5,

                                45,

                                CourseStatus.AVAILABLE

                        )

                ),

                LocalDateTime.of(2026, 7, 1, 12, 0)

        );

        given(marathonService.getMarathonDetail(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/marathons/{marathonId}", 1L)

                        .contentType(MediaType.APPLICATION_JSON))

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

                .andExpect(jsonPath("$.data.courses[0].remainingCount").value(90));

    }

    @Test

    @DisplayName("마라톤 상세 조회 실패 - 존재하지 않는 대회면 MARATHON_NOT_FOUND 예외 응답을 반환한다")

    void get_marathon_detail_fail_not_found() throws Exception {

        given(marathonService.getMarathonDetail(999L))

                .willThrow(new CustomException(ErrorCode.MARATHON_NOT_FOUND));

        mockMvc.perform(get("/api/v1/marathons/{marathonId}", 999L)

                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isNotFound())

                .andExpect(jsonPath("$.status").value(404))

                .andExpect(jsonPath("$.code").value("MARATHON_NOT_FOUND"))

                .andExpect(jsonPath("$.message").value(ErrorCode.MARATHON_NOT_FOUND.getMessage()));

    }

    @Test

    @DisplayName("마라톤 취소 성공 - 200 상태코드와 공통 응답 규격을 반환한다")

    void cancel_marathon_success() throws Exception {

        setAuthenticatedOrganizer(1L);

        CancelMarathonRes response = new CancelMarathonRes(

                10L,

                "서울 마라톤",

                LocalDate.of(2026, 10, 3),

                MarathonStatus.CANCELING,

                List.of(

                        new CancelCourseItemRes(101L, "5K"),

                        new CancelCourseItemRes(102L, "10K")

                )

        );

        given(marathonService.cancelMarathon(1L, 10L)).willReturn(response);

        mockMvc.perform(patch("/api/v1/marathons/{id}/cancel", 10L))

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

                .andExpect(jsonPath("$.data.courses[1].courseType").value("10K"));

        verify(marathonService).cancelMarathon(1L, 10L);

    }

    @Test

    @DisplayName("마라톤 수정 성공 - 201 상태코드와 공통 응답 규격을 반환한다")

    void update_marathon_success() throws Exception {

        setAuthenticatedOrganizer(1L);

        UpdateMarathonReq req = new UpdateMarathonReq(

                "수정된 서울 마라톤",

                "부산",
        "중구",
                LocalDate.of(2026, 11, 15),

                posterImage("updated-poster.png"),

                LocalDateTime.of(2026, 9, 1, 9, 0),

                LocalDateTime.of(2026, 9, 30, 18, 0),

                List.of(

                        new UpdateMarathonReq.UpdateCourseItemReq(

                                101L,

                                "5K",

                                BigDecimal.valueOf(35000),

                                120

                        ),

                        new UpdateMarathonReq.UpdateCourseItemReq(

                                102L,

                                "10K",

                                BigDecimal.valueOf(55000),

                                220

                        )

                )

        );

        UpdateMarathonRes res = new UpdateMarathonRes(

                10L,

                "수정된 서울 마라톤",

                "부산",
                        "중구",
                LocalDate.of(2026, 11, 15),

                "updated-poster.png",

                LocalDateTime.of(2026, 9, 1, 9, 0),

                LocalDateTime.of(2026, 9, 30, 18, 0),

                MarathonStatus.OPEN,

                List.of(

                        new CourseItemRes(

                                101L,

                                "5K",

                                BigDecimal.valueOf(35000),

                                120,

                                10,

                                110,

                                CourseStatus.AVAILABLE

                        ),

                        new CourseItemRes(

                                102L,

                                "10K",

                                BigDecimal.valueOf(55000),

                                220,

                                20,

                                200,

                                CourseStatus.AVAILABLE

                        )

                ),

                LocalDateTime.of(2026, 8, 1, 12, 0)

        );

        given(marathonService.updateMarathon(eq(1L), eq(10L), any(UpdateMarathonReq.class))).willReturn(res);

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/marathons/{marathonId}", 10L)
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
                        .param("courses[1].capacity", "220"))

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

                .andExpect(jsonPath("$.data.courses[1].courseType").value("10K"));

        ArgumentCaptor<UpdateMarathonReq> requestCaptor = ArgumentCaptor.forClass(UpdateMarathonReq.class);
        verify(marathonService).updateMarathon(eq(1L), eq(10L), requestCaptor.capture());

        UpdateMarathonReq capturedRequest = requestCaptor.getValue();
        assertEquals("수정된 서울 마라톤", capturedRequest.title());
        assertEquals("부산", capturedRequest.region());
        assertEquals("중구", capturedRequest.detailedAddress());
        assertEquals(LocalDate.of(2026, 11, 15), capturedRequest.eventDate());
        assertEquals("updated-poster.png", capturedRequest.posterImage().getOriginalFilename());
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 0), capturedRequest.registrationStartAt());
        assertEquals(LocalDateTime.of(2026, 9, 30, 18, 0), capturedRequest.registrationEndAt());
        assertEquals(2, capturedRequest.courses().size());
        assertEquals(101L, capturedRequest.courses().get(0).id());
        assertEquals("5K", capturedRequest.courses().get(0).courseType());
        assertEquals(BigDecimal.valueOf(35000), capturedRequest.courses().get(0).price());
        assertEquals(120, capturedRequest.courses().get(0).capacity());
        assertEquals(102L, capturedRequest.courses().get(1).id());
        assertEquals("10K", capturedRequest.courses().get(1).courseType());
        assertEquals(BigDecimal.valueOf(55000), capturedRequest.courses().get(1).price());
        assertEquals(220, capturedRequest.courses().get(1).capacity());

    }

    @Test

    @DisplayName("마라톤 수정 실패 - 코스 ID가 null이면 400 반환")

    void update_marathon_fail_course_id_null() throws Exception {

        setAuthenticatedOrganizer(1L);

        String request = """

        {

          "title": "수정된 서울 마라톤",

          "region": "부산",

          "eventDate": "2026-11-15",

          "posterImageUrl": "updated-poster.png",

          "registrationStartAt": "2026-09-01T09:00:00",

          "registrationEndAt": "2026-09-30T18:00:00",

          "courses": [

            {

              "id": null,

              "courseType": "5K",

              "price": 35000,

              "capacity": 120

            }

          ]

        }

        """;

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/marathons/{marathonId}", 10L)
                        .file(posterImage("updated-poster.png"))
                        .param("title", "수정된 서울 마라톤")
                        .param("region", "부산")
                        .param("detailedAddress", "중구")
                        .param("eventDate", "2026-11-15")
                        .param("registrationStartAt", "2026-09-01T09:00:00")
                        .param("registrationEndAt", "2026-09-30T18:00:00")
                        .param("courses[0].courseType", "5K")
                        .param("courses[0].price", "35000")
                        .param("courses[0].capacity", "120"))

                .andExpect(status().isBadRequest());

    }
    @Test
    @DisplayName("주최자 내 대회 조회 성공 - 200 상태코드와 공통 응답 규격을 반환한다")
    void get_my_marathons_success() throws Exception {
        setAuthenticatedOrganizer(1L);

        ReadMyMarathonRes response1 = new ReadMyMarathonRes(
                10L,
                "서울 마라톤",
                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),
                "poster.png",
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 31, 18, 0),
                MarathonStatus.OPEN,
                List.of(
                        new ReadMyMarathonRes.CourseSummary(
                                101L,
                                "5K",
                                BigDecimal.valueOf(30000),
                                100,
                                0
                        ),
                        new ReadMyMarathonRes.CourseSummary(
                                102L,
                                "10K",
                                BigDecimal.valueOf(50000),
                                200,
                                0
                        )
                )
        );

        ReadMyMarathonRes response2 = new ReadMyMarathonRes(
                11L,
                "부산 마라톤",
                "부산",
                "해운대구",
                LocalDate.of(2026, 11, 1),
                "poster2.png",
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 30, 18, 0),
                MarathonStatus.CANCELING,
                List.of(
                        new ReadMyMarathonRes.CourseSummary(
                                201L,
                                "HALF",
                                BigDecimal.valueOf(40000),
                                150,
                                10
                        )
                )
        );

        given(marathonService.getMyMarathons(1L))
                .willReturn(List.of(response1, response2));

        mockMvc.perform(get("/api/v1/marathons/me"))
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
                .andExpect(jsonPath("$.data[1].status").value("CANCELING"));

        verify(marathonService).getMyMarathons(1L);
    }
    //주최자 권한으로 인증된 사용자 설정 메서드
    private void setAuthenticatedOrganizer(Long userId) {

        SecurityUser securityUser = new SecurityUser(

                userId,

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

    private MockMultipartFile posterImage(String originalFilename) {
        return new MockMultipartFile(
                "posterImage",
                originalFilename,
                "image/png",
                "poster".getBytes()
        );
    }

}
