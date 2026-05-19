package com.rungo.api.domain.users.admin.dto

import com.rungo.api.domain.users.organizerApplication.entity.OrganizerApplication
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus

data class RejectOrganizerApplicationRes(
    val applicationId: Long,
    val userId: Long,
    val status: ApplicationStatus,
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