package com.rungo.api.domain.marathon.marathon.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonReq;
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonRes;
import com.rungo.api.domain.marathon.marathon.dto.delete.CancelMarathonRes;
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonDetailRes;
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonListRes;
import com.rungo.api.domain.marathon.marathon.dto.read.ReadMyMarathonRes;
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonReq;
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonRes;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.notification.event.MarathonCanceledEvent;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory;
import com.rungo.api.domain.registration.enumtype.RegistrationCancelReason;
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import com.rungo.api.global.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarathonService {
    private final MarathonRepository marathonRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationCancelHistoryRepository registrationCancelHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    @Value("${marathon.min-days.start-to-end}")
    private long minDaysBetweenStartAndEnd;

    @Value("${marathon.min-days.end-to-event}")
    private long minDaysBetweenEndAndEvent;

    @Transactional
    public CreateMarathonRes createMarathon(Long id, CreateMarathonReq req) {

        Users organizer = findOrganizer(id);

        String posterImageUrl = fileStorageService.saveMarathonPoster(req.posterImage());

        // 대회 접수 시작일이 종료일보다 이후이면 예외 처리
        if (req.registrationStartAt().isAfter(req.registrationEndAt())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 대회 개최일이 종료일 보다 이전이면 예외 처리
        if (req.eventDate().isBefore(req.registrationEndAt().toLocalDate())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        validateMarathonSchedule(
                req.registrationStartAt(),
                req.registrationEndAt(),
                req.eventDate()
        );

        //코스 타입 중복이면 예외 처리
        Set<String> courseTypes = new HashSet<>();
        for (CreateMarathonReq.CreateCourseItemReq courseReq : req.courses()) {


            if (!courseTypes.add(normalizeCourseType(courseReq.courseType()))) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        Marathon marathon = Marathon.create(
                organizer,
                req.title(),
                req.region(),
                req.detailedAddress(),
                req.eventDate(),
                posterImageUrl,
                req.registrationStartAt(),
                req.registrationEndAt()
        );

        for (CreateMarathonReq.CreateCourseItemReq courseReq : req.courses()) {
            Course course = new Course(
                    normalizeCourseType(courseReq.courseType()),
                    courseReq.price(),
                    courseReq.capacity(),
                    0
            );

            marathon.addCourse(course);
        }
        Marathon savedMarathon = marathonRepository.save(marathon);
        return CreateMarathonRes.from(savedMarathon);
    }

    @Transactional(readOnly = true)
    public MarathonDetailRes getMarathonDetail(Long marathonId) {
        Marathon marathon = getMarathonOrThrow(marathonId);
        if(marathon.getStatus() == MarathonStatus.CANCELED || marathon.getStatus() == MarathonStatus.CANCELING) {
            throw new CustomException(ErrorCode.MARATHON_CANCELED);
        }
        return MarathonDetailRes.from(marathon);
    }

    @Transactional(readOnly = true)
    public MarathonListRes getMarathons(Pageable pageable) {
        Page<Marathon> page = marathonRepository.findByStatusIn(
                List.of(MarathonStatus.TEMP, MarathonStatus.OPEN),
                pageable
        );
        return MarathonListRes.from(page);
    }

    @Transactional(readOnly = true)
    public List<ReadMyMarathonRes> getMyMarathons(Long userId){
        //실제 존재하는 유저인지 검증.
        Users organizer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        //그 유저의 Role이 Organizer인지 검증.
        if (organizer.getRole() != Role.ORGANIZER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return marathonRepository.findByOrganizerIdAndStatusNotIn(
                userId,
                List.of(MarathonStatus.CANCELING, MarathonStatus.CANCELED)
                )
                .stream()
                .map(ReadMyMarathonRes::from)
                .toList();
    }
    @Transactional
    public CancelMarathonRes cancelMarathon(Long id, Long marathonId){
        Users organizer = findOrganizer(id);

        Marathon marathon = getMarathonOrThrow(marathonId);

        //자기 자신이 신청한 마라톤만 취소할 수 있도록 예외 처리
        if(marathon.getOrganizer().getId() != organizer.getId()){
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 참가자 이메일 미리 조회 (N+1 방지용 JPQL 활용)
        List<String> participantEmails =
                registrationRepository.findParticipantEmailsByMarathonId(marathonId);
        List<Registration> registrations =
                registrationRepository.findAllByMarathon_IdOrderByAppliedAtDesc(marathonId);

        marathon.cancel();
        if (!registrations.isEmpty()) {
            List<RegistrationCancelHistory> cancelHistories = registrations.stream()
                    .map(registration -> RegistrationCancelHistory.create(
                            registration,
                            RegistrationCancelReason.MARATHON_CANCELED
                    ))
                    .toList();

            registrationCancelHistoryRepository.saveAll(cancelHistories);
            registrationRepository.deleteAll(registrations);
        }

        marathon.getCourses().forEach(Course::resetCurrentCount);

        // 참가자 있을 경우만 이벤트 발행
        if (!participantEmails.isEmpty()) {
            eventPublisher.publishEvent(
                    new MarathonCanceledEvent(
                            marathon.getTitle(),
                            participantEmails
                    )
            );
        }

        return CancelMarathonRes.from(marathon);

    }

    @Transactional
    public UpdateMarathonRes updateMarathon(Long organizerId, Long marathonId, UpdateMarathonReq req) {
        Marathon marathon = marathonRepository.findByIdAndOrganizer_Id(marathonId, organizerId)
                .orElseThrow(() -> new CustomException(ErrorCode.MARATHON_NOT_FOUND));

        //마라톤 접수 전까지만 수정 가능하도록 예외 처리
        if(!LocalDateTime.now().isBefore(marathon.getRegistrationStartAt())){
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if(marathon.isCanceled()){
            throw new CustomException(ErrorCode.MARATHON_ALREADY_CANCELED);
        }

        LocalDateTime registrationStartAt = req.registrationStartAt() != null
                ? req.registrationStartAt()
                : marathon.getRegistrationStartAt();

        LocalDateTime registrationEndAt = req.registrationEndAt() != null
                ? req.registrationEndAt()
                : marathon.getRegistrationEndAt();

        LocalDate eventDate = req.eventDate() != null
                ? req.eventDate()
                : marathon.getEventDate();

        validateMarathonSchedule(
                registrationStartAt,
                registrationEndAt,
                eventDate
        );

        String posterImageUrl = fileStorageService.saveMarathonPoster(req.posterImage());

        //기존에 있는 Course를 Map으로 저장
        Map<Long, Course> courseMap = toCourseMap(marathon);

        validateDuplicateCourseIds(req);
        validatePatchRequest(req, marathon);
        validateDuplicateCourseType(req, marathon,courseMap);


        marathon.updateMarathonInfo(
                req.title(),
                req.region(),
                req.detailedAddress(),
                req.eventDate(),
                posterImageUrl,
                req.registrationStartAt(),
                req.registrationEndAt()
        );

        //courses 가 NUll 이 아니라면 코스 수정 로직 수행, NULL 이면 코스 수정 없이 마라톤 정보만 업데이트
        if (req.courses() != null) {




            for (UpdateMarathonReq.UpdateCourseItemReq courseReq : req.courses()) {
                Course course = courseMap.get(courseReq.id()); //courseReq.id()는 DTO에서 NOT Null 제약을 걸었기에 따로 Null 처리하지 않았습니다.

                //수정 하고자 하는 Course 존재 안할시 예외처리
                if (course == null) {
                    throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
                }

                course.updateCourseInfo(
                        courseReq.courseType(),
                        courseReq.price(),
                        courseReq.capacity()
                );
            }
        }

        return UpdateMarathonRes.from(marathon);
    }



    // 5k -> 5K, 10k -> 10K, " 5k " -> 5K 로 저장하기 위해 정규화하는 함수
    private String normalizeCourseType(String courseType) {
        if (courseType == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return courseType.trim().toUpperCase();
    }

    private Marathon getMarathonOrThrow(Long marathonId){
        return marathonRepository.findById(marathonId)
                .orElseThrow(() -> new CustomException(ErrorCode.MARATHON_NOT_FOUND));
    }

    //id로 주최자 조회 함수, 존재하지 않거나 주최자가 아니면 예외 처리
    private Users findOrganizer(Long id){
        // 주최하는 사람이 존재하는지 확인
        Users organizer = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 주최자 측 인가 확인
        if (organizer.getRole() != Role.ORGANIZER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return organizer;
    }
    // 들어온 날짜를 토대로 유효성 검증하는 함수, null이 들어온 경우 기존 마라톤 정보를 토대로 검증
    private void validatePatchRequest(UpdateMarathonReq req, Marathon marathon) {
        LocalDateTime registrationStartAt = req.registrationStartAt() != null
                ? req.registrationStartAt()
                : marathon.getRegistrationStartAt();

        LocalDateTime registrationEndAt = req.registrationEndAt() != null
                ? req.registrationEndAt()
                : marathon.getRegistrationEndAt();

        LocalDate eventDate = req.eventDate() != null
                ? req.eventDate()
                : marathon.getEventDate();

        if (registrationStartAt.isAfter(registrationEndAt)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (eventDate.isBefore(registrationEndAt.toLocalDate())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    //코스 중복 여부 검사하는 함수
    private void validateDuplicateCourseType(UpdateMarathonReq req,
                                             Marathon marathon,
                                             Map<Long,Course> courseMap) {

        // 수정 요청에 코스가 없으면 검증 X
        if (req.courses() == null || req.courses().isEmpty()) {
            return;
        }

        // 먼저 기존 Course를 중복허용 하는 Map으로 저장.
        Map<Long, String> finalCourseTypes = marathon.getCourses().stream()

                .collect(Collectors.toMap(
                        Course::getId,
                        course -> normalizeCourseType(course.getCourseType())
                ));

        //수정 사항 반영.
        for (UpdateMarathonReq.UpdateCourseItemReq courseReq : req.courses()) {

            Course target = courseMap.get(courseReq.id());

            //만약 수정하려는 Course가 DB에 존재하지 않는다면 예외 처리
            if (target == null) {
                throw new CustomException(ErrorCode.COURSE_NOT_FOUND);
            }

            //null들어오면 수정 X
            if (courseReq.courseType() == null) continue;

            //중복을 허용하며, Map에 일단 저장
            finalCourseTypes.put(
                    courseReq.id(),
                    normalizeCourseType(courseReq.courseType())
            );
        }

        //Set에 기존에 만들었던 Map의 Value를 저장.
        // 이때 중복이 생긴다면, Set의 사이즈와 Map의 사이즈가 달라지는 것을 이용
        Set<String> uniqueTypes = new HashSet<>(finalCourseTypes.values());

        if (uniqueTypes.size() != finalCourseTypes.size()) {

            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    //코스 아이디 중복 여부 검사하는 함수.
    private void validateDuplicateCourseIds(UpdateMarathonReq req) {
        if (req.courses() == null || req.courses().isEmpty()) {
            return;
        }

        long distinctCount = req.courses().stream()
                .map(UpdateMarathonReq.UpdateCourseItemReq::id)
                .distinct()
                .count();

        if (distinctCount != req.courses().size()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    //기존 코스 리스트를 courseId를 key로 하는 Map으로 변환하여, 업데이트 요청에서 코스 아이디로 기존 코스 정보를 빠르게 조회할 수 있도록 함
    private Map<Long, Course> toCourseMap(Marathon marathon) {
        return marathon.getCourses().stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
    }

    //간격 사이에 최소 값 유효성 검사.
    private void validateMarathonSchedule(
            LocalDateTime registrationStartAt,
            LocalDateTime registrationEndAt,
            LocalDate eventDate
    ) {
        if (registrationStartAt.isAfter(registrationEndAt)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        long daysBetweenStartAndEnd =
                java.time.Duration.between(registrationStartAt, registrationEndAt).toDays();

        if (daysBetweenStartAndEnd < minDaysBetweenStartAndEnd) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        long daysBetweenEndAndEvent =
                java.time.Duration.between(registrationEndAt, eventDate.atStartOfDay()).toDays();

        if (daysBetweenEndAndEvent < minDaysBetweenEndAndEvent) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }


    }
    private void activateIfStarted(Marathon marathon) {
        if (marathon.getStatus() == MarathonStatus.TEMP
                && !LocalDateTime.now().isBefore(marathon.getRegistrationStartAt())) {
            marathon.open();
        }
    }
}
