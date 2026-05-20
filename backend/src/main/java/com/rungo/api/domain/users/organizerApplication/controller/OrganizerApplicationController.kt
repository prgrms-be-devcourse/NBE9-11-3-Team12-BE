package com.rungo.api.domain.users.organizerApplication.controller

import com.rungo.api.domain.users.organizerApplication.dto.OrganizerApplicationCreateReq
import com.rungo.api.domain.users.organizerApplication.dto.OrganizerApplicationCreateRes
import com.rungo.api.domain.users.organizerApplication.service.OrganizerApplicationService
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@Validated
@RequestMapping("/api/v1/organizer-applications")
@Tag(name = "Organizer Application", description = "주최자 권한 신청 API")
@SecurityRequirement(name = "accessTokenCookie")
class OrganizerApplicationController(
    private val organizerApplicationService: OrganizerApplicationService,
) {

    @PostMapping
    @Operation(summary = "주최자 권한 신청", description = "참가자가 주최자 권한을 신청합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "201", description = "주최자 권한 신청 성공"),
        SwaggerResponse(responseCode = "400", description = "잘못된 요청 또는 이미 대기 중인 신청 존재"),
        SwaggerResponse(responseCode = "403", description = "접근 권한 없음"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "404", description = "사용자 없음"),
    )
    fun requestApplication(
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @RequestBody req: OrganizerApplicationCreateReq,
    ): ResponseEntity<ApiResponse<OrganizerApplicationCreateRes>> {
        val response = organizerApplicationService.requestApplication(
            userId = user.id,
            req = req,
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created("주최자 권한신청 성공", response))
    }
}