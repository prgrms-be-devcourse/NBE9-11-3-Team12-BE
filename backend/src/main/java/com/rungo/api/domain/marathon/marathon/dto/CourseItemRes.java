package com.rungo.api.domain.marathon.marathon.dto;

import com.rungo.api.domain.marathon.course.entity.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import com.rungo.api.domain.marathon.course.status.CourseStatus;

import java.math.BigDecimal;

@Schema(description = "코스 정보 DTO")
public record CourseItemRes(

        @Schema(description = "코스 ID", example = "1")
        Long id,

        @Schema(description = "코스 종류", example = "10KM")
        String courseType,

        @Schema(description = "참가비", example = "50000")
        BigDecimal price,

        @Schema(description = "정원", example = "300")
        Integer capacity,

        @Schema(description = "현재 접수 인원", example = "120")
        Integer currentCount,

        @Schema(description = "잔여 인원", example = "180")
        Integer remainingCount,

        @Schema(description = "코스 상태", example = "AVAILABLE")
        CourseStatus status
) {
    public static CourseItemRes from(Course course) {
        return new CourseItemRes(
                course.getId(),
                course.getCourseType(),
                course.getPrice(),
                course.getCapacity(),
                course.getCurrentCount(),
                course.getRemainingCount(),
                course.getStatus()
        );
    }
}
