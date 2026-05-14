package com.rungo.api.domain.marathon.marathon.dto.delete

import com.rungo.api.domain.marathon.course.entity.Course
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "취소된 마라톤의 코스 정보 DTO")
@JvmRecord
data class CancelCourseItemRes(
    @field:Schema(description = "코스 ID", example = "1")
    val id: Long?,

    @field:Schema(description = "코스 타입", example = "10KM")
    val courseType: String?

) {
    companion object {
        @JvmStatic
        fun from(course: Course): CancelCourseItemRes {
            return CancelCourseItemRes(
                id =course.getId(),
                courseType = course.getCourseType()
            )
        }
    }
}
