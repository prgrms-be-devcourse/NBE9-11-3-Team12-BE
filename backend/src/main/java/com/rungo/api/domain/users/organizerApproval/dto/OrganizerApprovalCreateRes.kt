package com.rungo.api.domain.users.organizerApproval.dto

import com.rungo.api.domain.users.organizerApproval.entity.OrganizerApproval
import com.rungo.api.domain.users.organizerApproval.status.ApproveStatus
import java.time.LocalDateTime

data class OrganizerApprovalCreateRes(
    val id: Long,
    val userId: Long,
    val businessRegistrationNumber: String,
    val status: ApproveStatus,
    val requestedAt: LocalDateTime
) {
    companion object {
        fun from(organizerApproval: OrganizerApproval): OrganizerApprovalCreateRes =
            OrganizerApprovalCreateRes(
                id = organizerApproval.id,
                userId = organizerApproval.user.id,
                businessRegistrationNumber = organizerApproval.businessRegistrationNumber,
                status = organizerApproval.status,
                requestedAt = organizerApproval.requestedAt
            )
    }
}