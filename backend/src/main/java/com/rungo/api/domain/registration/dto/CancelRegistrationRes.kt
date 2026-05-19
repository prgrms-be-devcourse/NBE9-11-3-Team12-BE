package com.rungo.api.domain.registration.dto

import com.rungo.api.domain.registration.entity.Registration
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "접수 취소 응답 DTO")
data class CancelRegistrationRes(
    @field:Schema(description = "접수 ID", example = "10")
    val registrationId: Long,

    @field:Schema(description = "마라톤 ID", example = "1")
    val marathonId: Long,

    @field:Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
    val marathonTitle: String,

    @field:Schema(description = "코스 ID", example = "2")
    val courseId: Long,

    @field:Schema(description = "코스 종류", example = "10KM")
    val courseType: String,

    @field:Schema(description = "접수 상태", example = "CANCELED")
    val status: String,
) {
    companion object {
        fun from(registration: Registration) = CancelRegistrationRes(
            registrationId = registration.id,
            marathonId = registration.marathon.id,
            marathonTitle = registration.marathon.title,
            courseId = registration.course.id,
            courseType = registration.course.courseType,
            status = registration.status.name,
        )
    }
}
