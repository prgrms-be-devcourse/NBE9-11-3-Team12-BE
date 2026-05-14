package com.rungo.api.domain.registration.service;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.course.repository.CourseRepository;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository;
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent;
import com.rungo.api.domain.registration.dto.CreateRegistrationReq;
import com.rungo.api.domain.registration.dto.CreateRegistrationRes;
import com.rungo.api.domain.registration.dto.MyRegistrationRes;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory;
import com.rungo.api.domain.registration.enumtype.MyRegistrationStatusFilter;
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository;
import com.rungo.api.domain.registration.repository.RegistrationRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final RegistrationCancelHistoryRepository registrationCancelHistoryRepository;
    private final CourseRepository courseRepository;
    private final MarathonRepository marathonRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateRegistrationRes create(Long userId, CreateRegistrationReq request) {

        // 필수 약관 미동의
        if (!request.agreedTerms()) {
            throw new CustomException(ErrorCode.REGISTRATION_TERMS_REQUIRED);
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.isProfileCompleted()) {
            throw new CustomException(ErrorCode.PROFILE_NOT_COMPLETED);
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));
        Marathon marathon = course.getMarathon();
        LocalDateTime now = LocalDateTime.now();

        // 접수 기간이 아니면 생성할 수 없다.
        if (now.isBefore(marathon.getRegistrationStartAt()) || now.isAfter(marathon.getRegistrationEndAt())) {
            throw new CustomException(ErrorCode.REGISTRATION_PERIOD_INVALID);
        }
        // 모집 중인 대회만 접수 가능하다.
//        if (!marathon.isOpen()) {
//            throw new CustomException(ErrorCode.MARATHON_NOT_OPEN);
//        }
        //취소된 마라톤은 접수할 수 없다.
        if (marathon.isCanceled()) {
            throw new CustomException(ErrorCode.MARATHON_ALREADY_CANCELED);
        }

        Registration registration = Registration.create(
                user,
                course,
                marathon,
                request.snapZipCode(),
                request.snapAddress(),
                request.snapDetail(),
                request.tSize(),
                request.agreedTerms()
        );

        int updatedRows = courseRepository.increaseCurrentCountIfNotFull(course.getId());
        // 코스 정원이 가득 찼으면 접수를 막는다.
        if (updatedRows == 0) {
            throw new CustomException(ErrorCode.CAPACITY_FULL);
        }
        // 동시성 제어 미적용 메서드
        // course.increaseCurrentCount();
        Registration savedRegistration = registrationRepository.save(registration);

        eventPublisher.publishEvent(
                new RegistrationCompletedEvent(
                        user.getEmail(),
                        marathon.getTitle(),
                        course.getCourseType()
                )
        );

        return CreateRegistrationRes.from(savedRegistration);
    }

    public void cancel(Long userId, Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                // 존재하지 않는 접수 건은 취소할 수 없다.
                .orElseThrow(() -> new CustomException(ErrorCode.REGISTRATION_NOT_FOUND));

        // 본인 신청 건만 취소할 수 있다.
        if (!registration.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Marathon marathon = registration.getMarathon();

        LocalDateTime now = LocalDateTime.now();
        // 접수 마감 이후에는 취소할 수 없다.
        if (now.isAfter(marathon.getRegistrationEndAt())) {
            throw new CustomException(ErrorCode.REGISTRATION_CANCEL_PERIOD_INVALID);
        }
        // 모집 중인 대회만 취소할 수 있다.
//        if (!marathon.isOpen()) {
//            throw new CustomException(ErrorCode.MARATHON_NOT_OPEN);
//        }
        //취소된 마라톤은 접수 취소할 수 없다.
        if (marathon.isCanceled()) {
            throw new CustomException(ErrorCode.MARATHON_ALREADY_CANCELED);
        }
        //유니크 제약을 바로 확인하기 위해 saveAndFlush
        registrationCancelHistoryRepository.saveAndFlush(RegistrationCancelHistory.create(registration));

        courseRepository.decreaseCurrentCountIfPositive(registration.getCourse().getId());
        // 동시성 제어 미적용 메서드
        // registration.getCourse().decreaseCurrentCount();
        registrationRepository.delete(registration);

    }

    // status 필터에 따른 내 접수 목록 조회
    // ACTIVE   : 정상 접수인 상태 (취소 되지 않은 모든 접수)
    // CANCELED : 취소된 접수 상태
    @Transactional(readOnly = true)
    public MyRegistrationRes getMyRegistrations(Long userId, MyRegistrationStatusFilter status, Pageable pageable) {
        if (status == MyRegistrationStatusFilter.CANCELED) {
            return getCanceledRegistrations(userId, pageable);
        }

        return getActiveRegistrations(userId, pageable);
    }

    private MyRegistrationRes getActiveRegistrations(Long userId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("appliedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<Registration> page = registrationRepository.findByUser_Id(userId, sortedPageable);

        return MyRegistrationRes.fromActive(page);
    }

    private MyRegistrationRes getCanceledRegistrations(Long userId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("canceledAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<RegistrationCancelHistory> page = registrationCancelHistoryRepository.findByUserId(userId, sortedPageable);

        List<Long> marathonIds = page.getContent().stream()
                .map(RegistrationCancelHistory::getMarathonId)
                .distinct()
                .toList();

        List<Long> courseIds = page.getContent().stream()
                .map(RegistrationCancelHistory::getCourseId)
                .distinct()
                .toList();

        Map<Long, Marathon> marathonMap = marathonRepository.findAllById(marathonIds).stream()
                .collect(Collectors.toMap(Marathon::getId, Function.identity()));

        Map<Long, Course> courseMap = courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        return MyRegistrationRes.fromCanceled(page, marathonMap, courseMap);
    }
}
