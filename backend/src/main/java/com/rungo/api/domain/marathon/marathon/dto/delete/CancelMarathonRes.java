package com.rungo.api.domain.marathon.marathon.dto.delete;


import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "마라톤 취소 응답 DTO")
public record CancelMarathonRes(

        @Schema(description = "마라톤 ID", example = "1")
        Long marathonId,

        @Schema(description = "대회명", example = "2026 서울 마라톤")
        String title,

        @Schema(description = "대회 일자", example = "2020-02-02")
        LocalDate eventDate,

        @Schema(description = "마라톤 상태", example = "CANCELED")
        MarathonStatus status,

        @Schema(description = "코스 목록")
        List<CancelCourseItemRes> courses

) {
    public static CancelMarathonRes from(Marathon marathon) {
        return new CancelMarathonRes(
                marathon.getId(),
                marathon.getTitle(),
                marathon.getEventDate(),
                marathon.getStatus(),
                marathon.getCourses().stream()
                        .map(CancelCourseItemRes::from)
                        .toList()

        );
    }
}
