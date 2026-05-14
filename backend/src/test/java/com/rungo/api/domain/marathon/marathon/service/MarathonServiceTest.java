package com.rungo.api.domain.marathon.marathon.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonReq;
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonRes;
import com.rungo.api.domain.marathon.marathon.dto.delete.CancelMarathonRes;
import com.rungo.api.domain.marathon.marathon.dto.read.ReadMyMarathonRes;
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonReq;
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonRes;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.notification.event.MarathonCanceledEvent;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import com.rungo.api.global.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
@ExtendWith(MockitoExtension.class)
class MarathonServiceTest {

    @InjectMocks
    private MarathonService marathonService;

    @Mock
    private MarathonRepository marathonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RegistrationCancelHistoryRepository registrationCancelHistoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(marathonService, "minDaysBetweenStartAndEnd", 1L);
        ReflectionTestUtils.setField(marathonService, "minDaysBetweenEndAndEvent", 1L);
    }

    @Test
    @DisplayName("대회 취소 성공 시 참가자들에게 취소 알림 이벤트를 발행한다")
    void cancel_marathon_publish_event_success() {
        // given
        Users organizer = Users.builder()
                               .id(1L)
                               .role(Role.ORGANIZER)
                               .build();

        Marathon marathon = new Marathon(
                organizer,
                "서울 마라톤",
                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),
                "poster.png",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                MarathonStatus.OPEN
        );

        ReflectionTestUtils.setField(marathon, "id", 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer));
        given(marathonRepository.findById(1L)).willReturn(Optional.of(marathon));
        given(registrationRepository.findParticipantEmailsByMarathonId(1L))
                .willReturn(List.of("user1@test.com", "user2@test.com"));

        marathonService.cancelMarathon(1L, 1L);

        verify(eventPublisher, times(1))
                .publishEvent(any(MarathonCanceledEvent.class));
    }

    @Test

    @DisplayName("마라톤 생성 성공 - 저장과 응답 반환 및 코스 정규화가 정상 동작한다")

    void create_success() {

        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);

        CreateMarathonReq request = new CreateMarathonReq(

                "서울 마라톤",

                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),

                posterImage("poster.png"),

                LocalDateTime.of(2026, 8, 1, 9, 0),

                LocalDateTime.of(2026, 8, 31, 18, 0),

                List.of(

                        new CreateMarathonReq.CreateCourseItemReq("5k", new BigDecimal("30000"), 100),

                        new CreateMarathonReq.CreateCourseItemReq("10K", new BigDecimal("50000"), 200)

                )

        );

        given(userRepository.findById(organizerId)).willReturn(Optional.of(organizer));
        given(fileStorageService.saveMarathonPoster(any())).willReturn("poster.png");

        given(marathonRepository.save(any(Marathon.class))).willAnswer(invocation -> {

            Marathon saved = invocation.getArgument(0);

            ReflectionTestUtils.setField(saved, "id", 10L);

            return saved;

        });

        CreateMarathonRes result = marathonService.createMarathon(organizerId, request);

        ArgumentCaptor<Marathon> marathonCaptor = ArgumentCaptor.forClass(Marathon.class);

        verify(marathonRepository, times(1)).save(marathonCaptor.capture());

        Marathon capturedMarathon = marathonCaptor.getValue();

        assertSame(organizer, capturedMarathon.getOrganizer());

        assertEquals("서울 마라톤", capturedMarathon.getTitle());

        assertEquals("서울", capturedMarathon.getRegion());

        assertEquals(LocalDate.of(2026, 10, 3), capturedMarathon.getEventDate());

        assertEquals("poster.png", capturedMarathon.getPosterImageUrl());

        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), capturedMarathon.getRegistrationStartAt());

        assertEquals(LocalDateTime.of(2026, 8, 31, 18, 0), capturedMarathon.getRegistrationEndAt());

        assertEquals(MarathonStatus.OPEN, capturedMarathon.getStatus());

        assertEquals(2, capturedMarathon.getCourses().size());

        assertEquals("5K", capturedMarathon.getCourses().get(0).getCourseType());

        assertEquals(new BigDecimal("30000"), capturedMarathon.getCourses().get(0).getPrice());

        assertEquals(100, capturedMarathon.getCourses().get(0).getCapacity());

        assertEquals(0, capturedMarathon.getCourses().get(0).getCurrentCount());

        assertEquals("10K", capturedMarathon.getCourses().get(1).getCourseType());

        assertEquals(new BigDecimal("50000"), capturedMarathon.getCourses().get(1).getPrice());

        assertEquals(200, capturedMarathon.getCourses().get(1).getCapacity());

        assertEquals(0, capturedMarathon.getCourses().get(1).getCurrentCount());

        assertNotNull(result);

        assertEquals(10L, result.id());

        assertEquals("서울 마라톤", result.title());

        assertEquals("서울", result.region());

        assertEquals(LocalDate.of(2026, 10, 3), result.eventDate());

        assertEquals("poster.png", result.posterImageUrl());

        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), result.registrationStartAt());

        assertEquals(LocalDateTime.of(2026, 8, 31, 18, 0), result.registrationEndAt());

        assertEquals(MarathonStatus.OPEN, result.status());

        assertEquals(2, result.courses().size());

        assertEquals("5K", result.courses().get(0).courseType());

        assertEquals("10K", result.courses().get(1).courseType());

    }

    @Test

    @DisplayName("마라톤 생성 실패 - 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")

    void create_fail_user_not_found() {

        Long organizerId = 1L;

        CreateMarathonReq request = new CreateMarathonReq(

                "서울 마라톤",

                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),

                posterImage("poster.png"),

                LocalDateTime.of(2026, 8, 1, 9, 0),

                LocalDateTime.of(2026, 8, 31, 18, 0),

                List.of(

                        new CreateMarathonReq.CreateCourseItemReq("5K", new BigDecimal("30000"), 100)

                )

        );

        given(userRepository.findById(organizerId)).willReturn(Optional.empty());

        CustomException exception = assertThrows(

                CustomException.class,

                () -> marathonService.createMarathon(organizerId, request)

        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());

    }

    @Test

    @DisplayName("마라톤 생성 실패 - 주최자 권한이 아니면 FORBIDDEN 예외가 발생한다")

    void create_fail_not_organizer() {

        Long organizerId = 1L;

        Users participant = createUser(organizerId, "참가자", Role.PARTICIPANT);

        CreateMarathonReq request = new CreateMarathonReq(

                "서울 마라톤",

                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),

                posterImage("poster.png"),

                LocalDateTime.of(2026, 8, 1, 9, 0),

                LocalDateTime.of(2026, 8, 31, 18, 0),

                List.of(

                        new CreateMarathonReq.CreateCourseItemReq("5K", new BigDecimal("30000"), 100)

                )

        );

        given(userRepository.findById(organizerId)).willReturn(Optional.of(participant));

        CustomException exception = assertThrows(

                CustomException.class,

                () -> marathonService.createMarathon(organizerId, request)

        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());

    }

    @Test

    @DisplayName("마라톤 생성 실패 - 접수 시작일이 종료일보다 늦으면 INVALID_INPUT_VALUE 예외가 발생한다")

    void create_fail_registration_period_invalid() {

        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);

        CreateMarathonReq request = new CreateMarathonReq(

                "서울 마라톤",

                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),

                posterImage("poster.png"),

                LocalDateTime.of(2026, 9, 1, 9, 0),

                LocalDateTime.of(2026, 8, 31, 18, 0),

                List.of(

                        new CreateMarathonReq.CreateCourseItemReq("5K", new BigDecimal("30000"), 100)

                )

        );

        given(userRepository.findById(organizerId)).willReturn(Optional.of(organizer));

        CustomException exception = assertThrows(

                CustomException.class,

                () -> marathonService.createMarathon(organizerId, request)

        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());

    }

    @Test

    @DisplayName("마라톤 생성 실패 - 개최일이 접수 종료일보다 이르면 INVALID_INPUT_VALUE 예외가 발생한다")

    void create_fail_event_date_invalid() {

        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);

        CreateMarathonReq request = new CreateMarathonReq(

                "서울 마라톤",

                "서울",
                "성동구",
                LocalDate.of(2026, 8, 20),

                posterImage("poster.png"),

                LocalDateTime.of(2026, 8, 1, 9, 0),

                LocalDateTime.of(2026, 8, 31, 18, 0),

                List.of(

                        new CreateMarathonReq.CreateCourseItemReq("5K", new BigDecimal("30000"), 100)

                )

        );

        given(userRepository.findById(organizerId)).willReturn(Optional.of(organizer));

        CustomException exception = assertThrows(

                CustomException.class,

                () -> marathonService.createMarathon(organizerId, request)

        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());

    }

    @Test

    @DisplayName("마라톤 생성 실패 - 코스 타입이 정규화 후 중복되면 INVALID_INPUT_VALUE 예외가 발생한다")

    void create_fail_duplicate_course_type() {

        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);

        CreateMarathonReq request = new CreateMarathonReq(

                "서울 마라톤",

                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),

                posterImage("poster.png"),

                LocalDateTime.of(2026, 8, 1, 9, 0),

                LocalDateTime.of(2026, 8, 31, 18, 0),

                List.of(

                        new CreateMarathonReq.CreateCourseItemReq("5k", new BigDecimal("30000"), 100),

                        new CreateMarathonReq.CreateCourseItemReq(" 5K ", new BigDecimal("50000"), 200)

                )

        );

        given(userRepository.findById(organizerId)).willReturn(Optional.of(organizer));

        CustomException exception = assertThrows(

                CustomException.class,

                () -> marathonService.createMarathon(organizerId, request)

        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());

    }


    @Test
    @DisplayName("마라톤 생성 실패 - 접수 시작일과 종료일 간격이 1일 미만이면 예외 발생")
    void create_fail_start_end_interval() {

        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer));

        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 10, 0);

        LocalDateTime end = LocalDateTime.of(2026, 8, 1, 15, 0); // 같은 날 → 1일 미만

        CreateMarathonReq request = new CreateMarathonReq(

                "서울 마라톤",

                "서울",
                "성동구",
                LocalDate.of(2026, 8, 5),

                posterImage("poster.png"),

                LocalDateTime.of(2026, 8, 1, 10, 0),

                LocalDateTime.of(2026, 8, 1, 15, 0), // 최소 1일 미만

                List.of(

                        new CreateMarathonReq.CreateCourseItemReq(

                                "10K",

                                BigDecimal.valueOf(30000),

                                100

                        )

                )

        );

        CustomException exception = assertThrows(

                CustomException.class,

                () -> marathonService.createMarathon(1L, request)

        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());

    }

    @Test
    @DisplayName("마라톤 생성 실패 - 접수 종료일과 대회일 간격이 1일 미만이면 예외 발생")
    void create_fail_end_event_interval() {

        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer));

        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 10, 0);

        LocalDateTime end = LocalDateTime.of(2026, 8, 2, 10, 0);

        LocalDate eventDate = LocalDate.of(2026, 8, 2); // 종료일과 같은 날

        CreateMarathonReq request = new CreateMarathonReq(

                "서울 마라톤",

                "서울",
                "성동구",
                LocalDate.of(2026, 8, 4),

                posterImage("poster.png"),

                LocalDateTime.of(2026, 8, 1, 10, 0),

                LocalDateTime.of(2026, 8, 4, 15, 0),

                List.of(

                        new CreateMarathonReq.CreateCourseItemReq(

                                "10K",

                                BigDecimal.valueOf(30000),

                                100

                        )

                )

        );

        CustomException exception = assertThrows(

                CustomException.class,

                () -> marathonService.createMarathon(1L, request)

        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());

    }




    @Test
    @DisplayName("마라톤 상세 조회 실패 - 취소된 대회면 MARATHON_CANCELED 예외 발생")
    void get_marathon_detail_fail_canceled() {

         Marathon CanceledMarathon
             = new Marathon(
                    createUser(1L,"이순신",Role.ORGANIZER),
                    "서울 마라톤",
                    "서울",
                 "성동구",
                    LocalDate.of(2026, 10, 3),
                    "poster.png",
                    LocalDateTime.now().minusDays(10),
                    LocalDateTime.now().minusDays(5),
                    MarathonStatus.CANCELED
            );



        // given
        Marathon canceledMarathon = CanceledMarathon;

        given(marathonRepository.findById(1L))
                .willReturn(Optional.of(canceledMarathon));

        // when & then
        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.getMarathonDetail(1L)
        );

        assertEquals(ErrorCode.MARATHON_CANCELED, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 취소 성공 - 본인 대회를 취소하고 CANCELING 상태로 변경한다")
    void cancel_marathon_success() {
        Users organizer = createUser(1L, "주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, organizer, MarathonStatus.OPEN);

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer));
        given(marathonRepository.findById(10L)).willReturn(Optional.of(marathon));

        CancelMarathonRes result = marathonService.cancelMarathon(1L, 10L);

        assertNotNull(result);
        assertEquals(10L, result.marathonId());
        assertEquals("서울 마라톤", result.title());
        assertEquals(LocalDate.of(2026, 10, 3), result.eventDate());
        assertEquals(MarathonStatus.CANCELED, result.status());

        assertEquals(MarathonStatus.CANCELED, marathon.getStatus());
        assertEquals(2, result.courses().size());
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 주최자 유저가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void cancel_marathon_fail_user_not_found() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.cancelMarathon(1L, 10L)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 주최자 권한이 아니면 FORBIDDEN 예외가 발생한다")
    void cancel_marathon_fail_not_organizer() {
        Users participant = createUser(1L, "참가자", Role.PARTICIPANT);

        given(userRepository.findById(1L)).willReturn(Optional.of(participant));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.cancelMarathon(1L, 10L)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 존재하지 않는 대회면 MARATHON_NOT_FOUND 예외가 발생한다")
    void cancel_marathon_fail_marathon_not_found() {
        Users organizer = createUser(1L, "주최자", Role.ORGANIZER);

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer));
        given(marathonRepository.findById(10L)).willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.cancelMarathon(1L, 10L)
        );

        assertEquals(ErrorCode.MARATHON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 본인 대회가 아니면 FORBIDDEN 예외가 발생한다")
    void cancel_marathon_fail_forbidden() {
        Users organizer = createUser(1L, "주최자", Role.ORGANIZER);
        Users anotherOrganizer = createUser(2L, "다른주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, anotherOrganizer, MarathonStatus.OPEN);

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer));
        given(marathonRepository.findById(10L)).willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.cancelMarathon(1L, 10L)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 취소 실패 - 이미 취소된 대회면 MARATHON_ALREADY_CANCELED 예외가 발생한다")
    void cancel_marathon_fail_already_canceled() {
        Users organizer = createUser(1L, "주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, organizer, MarathonStatus.CANCELED);

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer));
        given(marathonRepository.findById(10L)).willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.cancelMarathon(1L, 10L)
        );

        assertEquals(ErrorCode.MARATHON_ALREADY_CANCELED, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 수정 성공 - 대회 기본 정보와 코스 정보가 정상 수정된다")
    void update_marathon_success() {
        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, organizer, MarathonStatus.OPEN);

        UpdateMarathonReq request = new UpdateMarathonReq(
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
                                "HALF",
                                BigDecimal.valueOf(40000),
                                150
                        ),
                        new UpdateMarathonReq.UpdateCourseItemReq(
                                102L,
                                "FULL",
                                BigDecimal.valueOf(70000),
                                300
                        )
                )
        );

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
                .willReturn(Optional.of(marathon));
        given(fileStorageService.saveMarathonPoster(any())).willReturn("updated-poster.png");

        UpdateMarathonRes result =
                marathonService.updateMarathon(organizerId, 10L, request);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("수정된 서울 마라톤", result.title());
        assertEquals("부산", result.region());
        assertEquals(LocalDate.of(2026, 11, 15), result.eventDate());
        assertEquals("updated-poster.png", result.posterImageUrl());
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 0), result.registrationStartAt());
        assertEquals(LocalDateTime.of(2026, 9, 30, 18, 0), result.registrationEndAt());

        assertEquals(2, result.courses().size());
        assertEquals("HALF", result.courses().get(0).courseType());
        assertEquals(BigDecimal.valueOf(40000), result.courses().get(0).price());
        assertEquals(150, result.courses().get(0).capacity());

        assertEquals("FULL", result.courses().get(1).courseType());
        assertEquals(BigDecimal.valueOf(70000), result.courses().get(1).price());
        assertEquals(300, result.courses().get(1).capacity());
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 본인 대회가 아니거나 존재하지 않으면 MARATHON_NOT_FOUND 예외가 발생한다")
    void update_marathon_fail_not_found() {
        Long organizerId = 1L;

        UpdateMarathonReq request = new UpdateMarathonReq(
                "수정된 서울 마라톤",
                "부산",
                 "중구",
                LocalDate.of(2026, 11, 15),
                posterImage("updated-poster.png"),
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 30, 18, 0),
                List.of()
        );

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
                .willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.updateMarathon(organizerId, 10L, request)
        );

        assertEquals(ErrorCode.MARATHON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 접수가 이미 시작된 대회면 INVALID_INPUT_VALUE 예외가 발생한다")
    void update_marathon_fail_registration_started() {
        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);

        Marathon marathon = new Marathon(
                organizer,
                "서울 마라톤",
                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),
                "poster.png",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5),
                MarathonStatus.OPEN
        );

        ReflectionTestUtils.setField(marathon, "id", 10L);

        UpdateMarathonReq request = new UpdateMarathonReq(
                "수정된 서울 마라톤",
                "부산",
                "중구",
                LocalDate.of(2026, 11, 15),
                posterImage("updated-poster.png"),
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 30, 18, 0),
                List.of()
        );

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
                .willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.updateMarathon(organizerId, 10L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 이미 취소된 대회면 MARATHON_ALREADY_CANCELED 예외가 발생한다")
    void update_marathon_fail_already_canceled() {
        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, organizer, MarathonStatus.CANCELED);

        UpdateMarathonReq request = new UpdateMarathonReq(
                "수정된 서울 마라톤",
                "부산",
                "중구",
                LocalDate.of(2026, 11, 15),
                posterImage("updated-poster.png"),
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 30, 18, 0),
                List.of()
        );

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
                .willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.updateMarathon(organizerId, 10L, request)
        );

        assertEquals(ErrorCode.MARATHON_ALREADY_CANCELED, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 접수 시작일이 종료일보다 늦으면 INVALID_INPUT_VALUE 예외가 발생한다")
    void update_marathon_fail_registration_period_invalid() {
        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, organizer, MarathonStatus.OPEN);

        UpdateMarathonReq request = new UpdateMarathonReq(
                "수정된 서울 마라톤",
                "부산",
                "중구",
                LocalDate.of(2026, 11, 15),
                posterImage("updated-poster.png"),
                LocalDateTime.of(2026, 9, 30, 18, 0),
                LocalDateTime.of(2026, 9, 1, 9, 0),
                List.of()
        );

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
                .willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.updateMarathon(organizerId, 10L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 개최일이 접수 종료일보다 이르면 INVALID_INPUT_VALUE 예외가 발생한다")
    void update_marathon_fail_event_date_invalid() {
        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, organizer, MarathonStatus.OPEN);

        UpdateMarathonReq request = new UpdateMarathonReq(
                "수정된 서울 마라톤",
                "부산",
                "중구",
                LocalDate.of(2026, 9, 10),
                posterImage("updated-poster.png"),
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 30, 18, 0),
                List.of()
        );

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
                .willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.updateMarathon(organizerId, 10L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 코스 ID가 중복되면 INVALID_INPUT_VALUE 예외가 발생한다")
    void update_marathon_fail_duplicate_course_ids() {
        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, organizer, MarathonStatus.OPEN);

        UpdateMarathonReq request = new UpdateMarathonReq(
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
                                "HALF",
                                BigDecimal.valueOf(40000),
                                150
                        ),
                        new UpdateMarathonReq.UpdateCourseItemReq(
                                101L,
                                "FULL",
                                BigDecimal.valueOf(70000),
                                300
                        )
                )
        );

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
                .willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.updateMarathon(organizerId, 10L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 존재하지 않는 코스를 수정하려 하면 COURSE_NOT_FOUND 예외가 발생한다")
    void update_marathon_fail_course_not_found() {
        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, organizer, MarathonStatus.OPEN);

        UpdateMarathonReq request = new UpdateMarathonReq(
                "수정된 서울 마라톤",
                "부산",
                "중구",
                LocalDate.of(2026, 11, 15),
                posterImage("updated-poster.png"),
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 30, 18, 0),
                List.of(
                        new UpdateMarathonReq.UpdateCourseItemReq(
                                999L,
                                "HALF",
                                BigDecimal.valueOf(40000),
                                150
                        )
                )
        );

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
                .willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.updateMarathon(organizerId, 10L, request)
        );

        assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("마라톤 수정 실패 - 코스 타입이 정규화 후 중복되면 INVALID_INPUT_VALUE 예외가 발생한다")
    void update_marathon_fail_duplicate_course_type() {
        Long organizerId = 1L;

        Users organizer = createUser(organizerId, "주최자", Role.ORGANIZER);
        Marathon marathon = createMarathon(10L, organizer, MarathonStatus.OPEN);

        UpdateMarathonReq request = new UpdateMarathonReq(
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
                                " 10k ",
                                BigDecimal.valueOf(40000),
                                150
                        ),
                        new UpdateMarathonReq.UpdateCourseItemReq(
                                102L,
                                "10K",
                                BigDecimal.valueOf(70000),
                                300
                        )
                )
        );

        given(marathonRepository.findByIdAndOrganizer_Id(10L, organizerId))
                .willReturn(Optional.of(marathon));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.updateMarathon(organizerId, 10L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
    @Test
    @DisplayName("주최자 내 대회 조회 성공 - 본인이 주최한 대회 목록을 반환한다")
    void get_my_marathons_success() {
        Users organizer = createUser(1L, "주최자", Role.ORGANIZER);

        Marathon marathon1 = createMarathon(10L, organizer, MarathonStatus.OPEN);
        Marathon marathon2 = createMarathon(11L, organizer, MarathonStatus.CANCELING);

        given(userRepository.findById(1L)).willReturn(Optional.of(organizer));
        given(marathonRepository.findByOrganizerIdAndStatusNotIn(1L, List.of(MarathonStatus.CANCELING, MarathonStatus.CANCELED)))
                .willReturn(List.of(marathon1, marathon2));

        List<ReadMyMarathonRes> result = marathonService.getMyMarathons(1L);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(10L, result.get(0).id());
        assertEquals("서울 마라톤", result.get(0).title());
        assertEquals("서울", result.get(0).region());
        assertEquals(MarathonStatus.OPEN, result.get(0).status());
        assertEquals(2, result.get(0).courses().size());
        assertEquals("5K", result.get(0).courses().get(0).courseType());
        assertEquals("10K", result.get(0).courses().get(1).courseType());

        assertEquals(11L, result.get(1).id());
        assertEquals(MarathonStatus.CANCELING, result.get(1).status());
    }

    @Test
    @DisplayName("주최자 내 대회 조회 실패 - 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void get_my_marathons_fail_user_not_found() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.getMyMarathons(1L)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("주최자 내 대회 조회 실패 - 주최자 권한이 아니면 FORBIDDEN 예외가 발생한다")
    void get_my_marathons_fail_not_organizer() {
        Users participant = createUser(1L, "참가자", Role.PARTICIPANT);

        given(userRepository.findById(1L)).willReturn(Optional.of(participant));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> marathonService.getMyMarathons(1L)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    private Users createUser(Long id, String name, Role role) {

        return Users.builder()

                .id(id)

                .email("test@test.com")

                .name(name)

                .phoneNumber("010-1111-2222")

                .role(role)

                .gender(Gender.MALE)

                .birth(LocalDate.of(2000, 1, 1))

                .build();

    }

    private MockMultipartFile posterImage(String originalFilename) {
        return new MockMultipartFile(
                "posterImage",
                originalFilename,
                "image/png",
                "poster".getBytes()
        );
    }

    private Marathon createMarathon(Long id, Users organizer, MarathonStatus status) {
        Marathon marathon = new Marathon(
                organizer,
                "서울 마라톤",
                "서울",
                "성동구",
                LocalDate.of(2026, 10, 3),
                "poster.png",
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 31, 18, 0),
                status
        );

        Course course1 = new Course(
                "5K",
                BigDecimal.valueOf(30000),
                100,
                0
        );

        Course course2 = new Course(
                "10K",
                BigDecimal.valueOf(50000),
                200,
                0
        );

        marathon.addCourse(course1);
        marathon.addCourse(course2);

        ReflectionTestUtils.setField(marathon, "id", id);
        ReflectionTestUtils.setField(course1, "id", 101L);
        ReflectionTestUtils.setField(course2, "id", 102L);

        return marathon;
    }
}
