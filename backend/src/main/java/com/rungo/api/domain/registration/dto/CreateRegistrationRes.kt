package com.rungo.api.domain.registration.dto

import com.rungo.api.domain.registration.entity.Registration
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "접수 생성 응답 DTO")
@JvmRecord
data class CreateRegistrationRes(
    @field:Schema(description = "접수 ID", example = "100")
    @JvmField val registrationId: Long,

    @field:Schema(description = "마라톤 ID", example = "1")
    @JvmField val marathonId: Long,

    @field:Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
    @JvmField val marathonTitle: String,

    @field:Schema(description = "코스 ID", example = "10")
    @JvmField val courseId: Long,

    @field:Schema(description = "코스 종류", example = "10KM")
    @JvmField val courseType: String,

    @field:Schema(description = "접수 상태", example = "COMPLETED")
    @JvmField val status: String,

    @field:Schema(description = "접수 시각", example = "2020-02-02T02:02:02")
    @JvmField val appliedAt: LocalDateTime,
) {
    companion object {
        @JvmStatic
        fun from(registration: Registration) = CreateRegistrationRes(
            registrationId = registration.id,
            marathonId = registration.marathon.id,
            marathonTitle = registration.marathon.title,
            courseId = registration.course.id,
            courseType = registration.course.courseType,
            status = registration.status.name,
            appliedAt = registration.appliedAt,
        )
    }
}
