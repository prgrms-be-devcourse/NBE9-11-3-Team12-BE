package com.rungo.api.domain.marathon.marathon.dto.read

import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "내가 생성한 마라톤 조회 응답 DTO")
data class ReadMyMarathonRes(

    @field:Schema(description = "마라톤 ID", example = "1")
    @JvmField
    val id: Long,

    @field:Schema(description = "대회명", example = "2026 서울 마라톤")
    @JvmField
    val title: String,

    @field:Schema(description = "지역", example = "서울")
    @JvmField
    val region: String,

    @field:Schema(description = "상세주소", example = "서울특별시 송파구 올림픽로 424")
    @JvmField
    val detailedAddress: String,

    @field:Schema(description = "대회 일자", example = "2020-02-02")
    @JvmField
    val eventDate: LocalDate,

    @field:Schema(description = "포스터 이미지 URL", example = "https://example.com/poster.png")
    @JvmField
    val posterImageUrl: String?,

    @field:Schema(description = "접수 시작 일시", example = "2020-02-02T02:02:02")
    @JvmField
    val registrationStartAt: LocalDateTime,

    @field:Schema(description = "접수 종료 일시", example = "2020-02-02T02:02:02")
    @JvmField
    val registrationEndAt: LocalDateTime,

    @field:Schema(description = "마라톤 상태", example = "OPEN")
    @JvmField
    val status: MarathonStatus,

    @field:Schema(description = "코스 요약 목록")
    @JvmField
    val courses: List<CourseSummary>
) {
    @Schema(description = "코스 요약 DTO")
    data class CourseSummary(

        @field:Schema(description = "코스 ID", example = "1")
        @JvmField
        val courseId: Long,

        @field:Schema(description = "코스 타입", example = "10KM")
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
        val currentCount: Int
    )

    companion object {
        @JvmStatic
        fun from(marathon: Marathon) = ReadMyMarathonRes(
            id = marathon.id,
            title = marathon.title,
            region = marathon.region,
            detailedAddress = marathon.detailedAddress,
            eventDate = marathon.eventDate,
            posterImageUrl = marathon.posterImageUrl,
            registrationStartAt = marathon.registrationStartAt,
            registrationEndAt = marathon.registrationEndAt,
            status = marathon.status,
            courses = marathon.courses
                .map { course ->
                    CourseSummary(
                        courseId = course.id,
                        courseType = course.courseType,
                        price = course.price,
                        capacity = course.capacity,
                        currentCount = course.currentCount
                    )
                }
        )

    }
}