package com.rungo.api.domain.marathon.marathon.dto

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.status.CourseStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "코스 정보 DTO")
data class CourseItemRes(

    @field:Schema(description = "코스 ID", example = "1")
    @JvmField
    val id: Long,

    @field:Schema(description = "코스 종류", example = "10KM")
    @JvmField
    val courseType: String,

    @field:Schema(description = "참가비", example = "50000")
    @JvmField
    val price: BigDecimal,

    @field:Schema(description = "정원", example = "300")
    @JvmField
    val capacity: Int,

    @field:Schema(description = "현재 접수 인원", example = "120")
    @JvmField
    val currentCount: Int,

    @field:Schema(description = "잔여 인원", example = "180")
    @JvmField
    val remainingCount: Int,

    @field:Schema(description = "코스 상태", example = "AVAILABLE")
    @JvmField
    val status: CourseStatus
) {

    companion object {

        @JvmStatic
        fun from(course: Course) = CourseItemRes(
            id = course.id,
            courseType = course.courseType,
            price = course.price,
            capacity = course.capacity,
            currentCount = course.currentCount,
            remainingCount = course.remainingCount,
            status = course.status
        )
    }

}