package com.rungo.api.domain.registration.dto;


import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.marathon.dto.PageRes;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "내 접수 목록 조회 응답 DTO")
public record MyRegistrationRes(

        @Schema(description = "접수 목록")
        List<Item> content,

        @Schema(description = "페이지 정보")
        PageRes pageRes
) {

    // 정상 접수 목록 DTO 변환
    public static MyRegistrationRes fromActive(Page<Registration> page) {
        return new MyRegistrationRes(
                page.getContent().stream()
                        .map(Item::fromActive)
                        .toList(),
                PageRes.from(page)
        );
    }

    // 접수 취소 목록 DTO 변환
    public static MyRegistrationRes fromCanceled(
            Page<RegistrationCancelHistory> page,
            Map<Long, Marathon> marathonMap,
            Map<Long, Course> courseMap
    ) {
        return new MyRegistrationRes(
                page.getContent().stream()
                        .map(history -> Item.fromCanceled(
                                history,
                                marathonMap.get(history.getMarathonId()),
                                courseMap.get(history.getCourseId())
                        ))
                        .toList(),
                PageRes.from(page)
        );
    }

    // ACTIVE, CANCELED 공통 구조 => 일부 필드는 null
    @Schema(description = "내 접수 목록 항목 DTO")
    public record Item(
            @Schema(description = "현재 접수 ID 또는 취소 이력 ID", example = "100")
            Long registrationId,

            @Schema(description = "원본 접수 ID(취소 이력 조회 시 사용)", example = "200")
            Long historyId,

            @Schema(description = "마라톤 ID", example = "1")
            Long marathonId,

            @Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
            String marathonTitle,

            @Schema(description = "코스 ID", example = "10")
            Long courseId,

            @Schema(description = "코스 종류", example = "10KM")
            String courseType,

            @Schema(description = "접수 상태", example = "COMPLETED")
            String status,

            @Schema(description = "결제 금액", example = "50000")
            BigDecimal price,

            @Schema(description = "대회 날짜", example = "2020-02-02")
            LocalDate eventDate,

            @Schema(description = "접수 이름", example = "홍길동")
            String snapName,

            @Schema(description = "접수 전화번호", example = "010-1234-5678")
            String snapPhoneNumber,

            @Schema(description = "접수 우편번호", example = "12345")
            String snapZipCode,

            @Schema(description = "접수 주소", example = "서울시 강남구 ...")
            String snapAddress,

            @Schema(description = "접수 상세주소", example = "101동 202호")
            String snapDetail,

            @Schema(description = "티셔츠 사이즈", example = "L")
            String tSize,

            @Schema(description = "약관 동의 여부", example = "true")
            Boolean agreedTerms,

            @Schema(description = "접수 시각", example = "2020-02-02T02:02:02")
            LocalDateTime appliedAt,

            @Schema(description = "취소 시각", example = "2020-02-02T02:02:02")
            LocalDateTime canceledAt
    ) {
        // 정상 접수 Entity를 응답용 item 변환 (취소 전용 필드는 null : originalRegistrationId, canceledAt)
        public static Item fromActive(Registration registration) {
            return new Item(
                    registration.getId(),
                    null,
                    registration.getMarathon().getId(),
                    registration.getMarathon().getTitle(),
                    registration.getCourse().getId(),
                    registration.getCourse().getCourseType(),
                    "ACTIVE",
                    registration.getCourse().getPrice(),
                    registration.getMarathon().getEventDate(),

                    registration.getSnapName(),
                    registration.getSnapPhoneNumber(),
                    registration.getSnapZipCode(),
                    registration.getSnapAddress(),
                    registration.getSnapDetail(),
                    registration.getTSize(),
                    registration.isAgreedTerms(),

                    registration.getAppliedAt(),
                    null
            );
        }

        // 취소 접수 Entity를 응답용 item 변환
        public static Item fromCanceled(
                RegistrationCancelHistory history,
                Marathon marathon,
                Course course
        ) {
            return new Item(
                    history.getId(),
                    history.getOriginalRegistrationId(),
                    history.getMarathonId(),
                    marathon != null ? marathon.getTitle() : null,
                    history.getCourseId(),
                    course != null ? course.getCourseType() : null,
                    "CANCELED",
                    course != null ? course.getPrice() : null,
                    marathon != null ? marathon.getEventDate() : null,

                    history.getSnapName(),
                    history.getSnapPhoneNumber(),
                    history.getSnapZipCode(),
                    history.getSnapAddress(),
                    history.getSnapDetail(),
                    history.getTSize(),
                    history.isAgreedTerms(),

                    history.getAppliedAt(),
                    history.getCanceledAt()
            );
        }
    }
}