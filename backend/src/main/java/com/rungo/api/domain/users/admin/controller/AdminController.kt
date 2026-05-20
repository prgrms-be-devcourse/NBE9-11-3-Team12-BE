package com.rungo.api.domain.users.admin.controller

import com.rungo.api.domain.marathon.marathon.service.MarathonCleanupService
import com.rungo.api.domain.users.admin.dto.AdminApproveRes
import com.rungo.api.domain.users.admin.dto.AdminOrganizerApplicationListRes
import com.rungo.api.domain.users.admin.dto.RejectOrganizerApplicationReq
import com.rungo.api.domain.users.admin.dto.RejectOrganizerApplicationRes
import com.rungo.api.domain.users.admin.service.AdminService
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@Validated
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "관리자 API")
@SecurityRequirement(name = "accessTokenCookie")
class AdminController(
    private val adminService: AdminService,
    private val marathonCleanupService: MarathonCleanupService,
) {


    @PatchMapping("/organizer-applications/{applicationId}/approve")
    @Operation(summary = "주최자 권한 신청 승인", description = "관리자가 주최자 권한 신청을 승인합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "승인 성공"),
        SwaggerResponse(responseCode = "400", description = "이미 처리된 신청 또는 이미 주최자 권한 보유"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "403", description = "관리자 권한 필요"),
        SwaggerResponse(responseCode = "404", description = "주최자 권한 신청 없음"),
    )
    fun approveOrganizerApplication(
        @AuthenticationPrincipal admin: SecurityUser,
        @PathVariable applicationId: Long,
    ): ResponseEntity<ApiResponse<AdminApproveRes>> {
        val response = adminService.approveOrganizerApplication(
            adminId = admin.id,
            applicationId = applicationId,
        )

        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PatchMapping("/organizer-applications/{applicationId}/reject")
    @Operation(summary = "주최자 권한 거절", description = "관리자가 특정 사용자의 주최자 권한 신청을 거절합니다 .")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "주최자 권한 신청 거절 성공"),
        SwaggerResponse(responseCode = "400", description = "이미 처리된 신청 또는 잘못된 요청"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "403", description = "관리자 권한 필요"),
        SwaggerResponse(responseCode = "404", description = "주최자 권한 신청 없음"),
    )
    fun rejectOrganizerApplication(
        @AuthenticationPrincipal admin: SecurityUser,
        @PathVariable applicationId: Long,
        @Valid @RequestBody req: RejectOrganizerApplicationReq,
    ): ResponseEntity<ApiResponse<RejectOrganizerApplicationRes>> {
        val response = adminService.rejectOrganizerApplication(
            adminId = admin.id,
            applicationId = applicationId,
            req = req,
        )

        return ResponseEntity.ok(
            ApiResponse.ok(response)
        )
    }

    @PostMapping("/cleanup")
    @Operation(summary = "데이터 초기화", description = "기간이 지난 대회, 접수 내역, 취소 내역을 삭제합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "초기화 성공"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "403", description = "관리자 권한 필요"),
    )
    fun cleanup(): ResponseEntity<ApiResponse<Void?>> {
        marathonCleanupService.cleanup()
        return ResponseEntity.ok(ApiResponse.okMessage("데이터 초기화가 완료되었습니다."))
    }

    @Operation(summary = "주최자 권한 신청 목록 조회", description = "관리자가 주최자 권한 신청 목록을 조회합니다. status 값으로 상태별 필터링이 가능합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "조회 성공"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "403", description = "관리자 권한 필요"),
        SwaggerResponse(responseCode = "404", description = "관리자 사용자 없음"),
    )
    @GetMapping("/organizer-applications")
    fun getOrganizerApplications(
        @AuthenticationPrincipal admin: SecurityUser,
        @RequestParam(required = false) status: ApplicationStatus?,
        @RequestParam(defaultValue = "0")
        @Min(value = 0, message = "page는 0 이상이어야 합니다.")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        size: Int,
        ): ResponseEntity<ApiResponse<AdminOrganizerApplicationListRes>> {
        val pageable: Pageable = PageRequest.of(page, size)

        val result = adminService.getOrganizerApplications(
            adminId = admin.id,
            status = status,
            pageable = pageable,
        )

        return ResponseEntity.ok(ApiResponse.ok(result))
    }
}
