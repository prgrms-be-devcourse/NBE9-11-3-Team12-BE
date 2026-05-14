package com.rungo.api.domain.marathon.marathon.dto.read;


import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.marathon.dto.PageRes;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import com.rungo.api.domain.marathon.marathon.enumtype.RecruitmentStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "마라톤 목록 조회 응답 DTO")
public record MarathonListRes(

        @Schema(description = "마라톤 목록")
        List<Item> content,

        @Schema(description = "페이지 정보")
        PageRes pageRes

) {

    public static MarathonListRes from(Page<Marathon> page) {
        return new MarathonListRes(
                page.getContent().stream()
                        .map(Item::from)
                        .toList(),
                PageRes.from(page)
        );
    }

    @Schema(description = "마라톤 목록 항목 DTO")
    public record Item(

            @Schema(description = "마라톤 ID", example = "1")
            Long id,

            @Schema(description = "대회명", example = "2026 서울 마라톤")
            String title,

            @Schema(description = "지역", example = "서울")
            String region,

            @Schema(description = "상세주소", example = "서울특별시 송파구 올림픽로 424")
            String detailedAddress,

            @Schema(description = "대회 일자", example = "2020-02-02")
            LocalDate eventDate,

            @Schema(description = "포스터 이미지 URL", example = "https://example.com/poster.png")
            String posterImageUrl,

            @Schema(description = "접수 시작 일시", example = "2020-02-02T02:02:02")
            LocalDateTime registrationStartAt,

            @Schema(description = "접수 종료 일시", example = "2020-02-02T02:02:02")
            LocalDateTime registrationEndAt,

            @Schema(description = "마라톤 상태", example = "OPEN")
            MarathonStatus status,

            @Schema(description = "전체 정원", example = "500")
            int totalCapacity,

            @Schema(description = "전체 접수 인원", example = "180")
            int totalCurrentCount,

            @Schema(description = "모집 상태", example = "OPEN")
            RecruitmentStatus recruitmentStatus

    ) {
        public static Item from(Marathon marathon) {
            int totalCapacity = marathon.getCourses().stream()
                    .mapToInt(Course::getCapacity)
                    .sum();

            int totalCurrentCount = marathon.getCourses().stream()
                    .mapToInt(Course::getCurrentCount)
                    .sum();
            RecruitmentStatus recruitmentStatus = calculateStatus(
                    marathon,
                    totalCapacity,
                    totalCurrentCount
            );
            return new Item(

                    marathon.getId(),
                    marathon.getTitle(),
                    marathon.getRegion(),
                    marathon.getDetailedAddress(),
                    marathon.getEventDate(),
                    marathon.getPosterImageUrl(),
                    marathon.getRegistrationStartAt(),
                    marathon.getRegistrationEndAt(),
                    marathon.getStatus(),
                    totalCapacity,
                    totalCurrentCount,
                    recruitmentStatus
            );
        }
    }
    private static RecruitmentStatus calculateStatus(
            Marathon marathon,
            int totalCapacity,
            int totalCurrentCount
    ) {
        LocalDateTime now = LocalDateTime.now();
        if (marathon.isCanceled()) {
            return RecruitmentStatus.CANCELED;
        }
        if (now.isBefore(marathon.getRegistrationStartAt())) {
            return RecruitmentStatus.TEMP; // or UPCOMING 추천
        }
        if (now.isAfter(marathon.getRegistrationEndAt())) {
            return RecruitmentStatus.CLOSED;
        }
        if (totalCurrentCount >= totalCapacity) {
            return RecruitmentStatus.FULL;
        }
        return RecruitmentStatus.OPEN;
    }
}
