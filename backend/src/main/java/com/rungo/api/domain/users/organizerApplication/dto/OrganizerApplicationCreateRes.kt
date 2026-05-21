package com.rungo.api.domain.users.organizerApplication.dto

import com.rungo.api.domain.users.organizerApplication.entity.OrganizerApplication
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "주최자 권한 신청 응답 DTO")
data class OrganizerApplicationCreateRes(
    @field:Schema(description = "주최자 권한 신청 ID", example = "1")
    val id: Long,

    @field:Schema(description = "신청 사용자 ID", example = "10")
    val userId: Long,

    @field:Schema(description = "사업자 등록번호", example = "123-45-67890")
    val businessRegistrationNumber: String,

    @field:Schema(description = "신청 상태", example = "PENDING")
    val status: ApplicationStatus,

    @field:Schema(description = "신청 시각", example = "2020-02-02T02:02:02")
    val requestedAt: LocalDateTime,
) {
    companion object {
        fun from(organizerApplication: OrganizerApplication): OrganizerApplicationCreateRes =
            OrganizerApplicationCreateRes(
                id = organizerApplication.id,
                userId = organizerApplication.user.id,
                businessRegistrationNumber = organizerApplication.businessRegistrationNumber,
                status = organizerApplication.status,
                requestedAt = organizerApplication.requestedAt,
            )
    }
}
