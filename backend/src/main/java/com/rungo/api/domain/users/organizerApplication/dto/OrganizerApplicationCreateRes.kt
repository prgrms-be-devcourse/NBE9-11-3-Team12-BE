package com.rungo.api.domain.users.organizerApplication.dto

import com.rungo.api.domain.users.organizerApplication.entity.OrganizerApplication
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import java.time.LocalDateTime

data class OrganizerApplicationCreateRes(
    val id: Long,
    val userId: Long,
    val businessRegistrationNumber: String,
    val status: ApplicationStatus,
    val requestedAt: LocalDateTime
) {
    companion object {
        fun from(organizerApplication: OrganizerApplication): OrganizerApplicationCreateRes =
            OrganizerApplicationCreateRes(
                id = organizerApplication.id,
                userId = organizerApplication.user.id,
                businessRegistrationNumber = organizerApplication.businessRegistrationNumber,
                status = organizerApplication.status,
                requestedAt = organizerApplication.requestedAt
            )
    }
}