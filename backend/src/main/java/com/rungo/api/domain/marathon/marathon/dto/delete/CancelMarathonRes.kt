package com.rungo.api.domain.marathon.marathon.dto.delete

import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "마라톤 취소 응답 DTO")
data class CancelMarathonRes(

    @field:Schema(description = "마라톤 ID", example = "1")
    val marathonId: Long,

    @field:Schema(description = "대회명", example = "2026 서울 마라톤")
    val title: String,

    @field:Schema(description = "대회 일자", example = "2020-02-02")
    val eventDate: LocalDate,

    @field:Schema(description = "마라톤 상태", example = "CANCELED")
    val status: MarathonStatus,

    @field:Schema(description = "코스 목록")
    val courses: List<CancelCourseItemRes>
) {
    companion object {
        fun from(marathon: Marathon) = CancelMarathonRes(
            marathonId = marathon.id,
            title = marathon.title,
            eventDate = marathon.eventDate,
            status = marathon.status,
            courses = marathon.courses
                .map { CancelCourseItemRes.from(it) }
        )

    }
}