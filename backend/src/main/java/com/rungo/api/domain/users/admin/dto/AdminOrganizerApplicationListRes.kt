package com.rungo.api.domain.users.admin.dto

import com.rungo.api.domain.users.organizerApplication.entity.OrganizerApplication
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.time.LocalDateTime

data class AdminOrganizerApplicationListRes(
    @field:Schema(description = "주최자 권한 신청 목록")
    val content: List<Item>,

    @field:Schema(description = "페이지 정보")
    val page: PageInfo,
) {
    data class Item(
        @field:Schema(description = "주최자 권한 신청 ID", example = "1")
        val applicationId: Long,

        @field:Schema(description = "주최자 권한 신청한 사용자 ID", example = "1")
        val userId: Long,

        @field:Schema(description = "주최자 권한 신청한 사용자 이름", example = "홍길동")
        val userName: String,

        @field:Schema(description = "주최자 권한 신청한 사용자 이메일", example = "Example@com")
        val userEmail: String,

        @field:Schema(description = "사업자 등록 번호", example = "123-45-67890")
        val businessRegistrationNumber: String,

        @field:Schema(description = "주최자 권한 신청 상태", example = "PENDING")
        val status: ApplicationStatus,

        @field:Schema(description = "주최자 권한 신청 거절 사유", example = "사업자 등록 번호가 유효하지 않습니다.")
        val rejectReason: String?,

        @field:Schema(description = "주최자 권한 신청 요청 시간", example = "2024-01-01T12:00:00")
        val requestedAt: LocalDateTime,

    ) {
        companion object {
            fun from(application: OrganizerApplication): Item =
                Item(
                    applicationId = application.id,
                    userId = application.user.id,
                    userName = application.user.name,
                    userEmail = application.user.email,
                    businessRegistrationNumber = application.businessRegistrationNumber,
                    status = application.status,
                    rejectReason = application.rejectReason,
                    requestedAt = application.requestedAt,
                )
        }
    }

    data class PageInfo(
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )
    companion object {
        fun from(page: Page<OrganizerApplication>): AdminOrganizerApplicationListRes =
            AdminOrganizerApplicationListRes(
                content = page.content.map { Item.from(it) },
                page = PageInfo(
                    page = page.number,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                ),
            )
    }
}