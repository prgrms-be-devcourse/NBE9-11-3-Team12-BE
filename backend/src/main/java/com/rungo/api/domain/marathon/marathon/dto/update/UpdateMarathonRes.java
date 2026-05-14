package com.rungo.api.domain.marathon.marathon.dto.update;

import com.rungo.api.domain.marathon.marathon.dto.CourseItemRes;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "마라톤 수정 응답 DTO")
public record UpdateMarathonRes(

        @Schema(description = "마라톤 ID", example = "1")
        Long id,

        @Schema(description = "대회명", example = "2026 서울 국제 마라톤")
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

        @Schema(description = "코스 목록")
        List<CourseItemRes> courses,

        @Schema(description = "수정 시각", example = "2020-02-02T02:02:02")
        LocalDateTime updatedAt

) {
    public static UpdateMarathonRes from(Marathon marathon) {
        return new UpdateMarathonRes(
                marathon.getId(),
                marathon.getTitle(),
                marathon.getRegion(),
                marathon.getDetailedAddress(),
                marathon.getEventDate(),
                marathon.getPosterImageUrl(),
                marathon.getRegistrationStartAt(),
                marathon.getRegistrationEndAt(),
                marathon.getStatus(),
                marathon.getCourses().stream()
                        .map(CourseItemRes::from)
                        .toList(),
                LocalDateTime.now()
        );
    }
}

