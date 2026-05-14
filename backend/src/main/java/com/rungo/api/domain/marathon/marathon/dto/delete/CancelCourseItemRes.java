package com.rungo.api.domain.marathon.marathon.dto.delete;

import com.rungo.api.domain.marathon.course.entity.Course;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "취소된 마라톤의 코스 정보 DTO")
public record CancelCourseItemRes(

        @Schema(description = "코스 ID", example = "1")
        Long id,

        @Schema(description = "코스 타입", example = "10KM")
        String courseType

) {
    public static CancelCourseItemRes from(Course course){
        return new CancelCourseItemRes(
                course.getId(),
                course.getCourseType()
        );
    }
}
