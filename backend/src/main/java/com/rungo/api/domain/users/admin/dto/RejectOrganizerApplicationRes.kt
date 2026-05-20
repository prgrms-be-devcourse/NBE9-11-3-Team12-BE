package com.rungo.api.domain.users.admin.dto

import com.rungo.api.domain.users.organizerApplication.entity.OrganizerApplication
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import io.swagger.v3.oas.annotations.media.Schema

data class RejectOrganizerApplicationRes(
    @field:Schema(description = "신청 ID", example = "10")
    val applicationId: Long,

    @field:Schema(description = "사용자 ID", example = "1")
    val userId: Long,

    @field:Schema(description = "신청 상태", example = "REJECTED")
    val status: ApplicationStatus,

    @field:Schema(description = "거절 사유", example = "사업자등록번호가 유효하지 않습니다.")
    val rejectReason: String?,
) {
    companion object {
        fun from(application: OrganizerApplication): RejectOrganizerApplicationRes =
            RejectOrganizerApplicationRes(
                applicationId = application.id,
                userId = application.user.id,
                status = application.status,
                rejectReason = application.rejectReason,
            )
    }
}