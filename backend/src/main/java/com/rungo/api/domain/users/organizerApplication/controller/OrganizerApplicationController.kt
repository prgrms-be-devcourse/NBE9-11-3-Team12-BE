package com.rungo.api.domain.users.organizerApplication.controller

import com.rungo.api.domain.users.organizerApplication.dto.OrganizerApplicationCreateReq
import com.rungo.api.domain.users.organizerApplication.dto.OrganizerApplicationCreateRes
import com.rungo.api.domain.users.organizerApplication.service.OrganizerApplicationService
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("/api/v1/organizer-applications")
class OrganizerApplicationController(
    private val organizerApplicationService: OrganizerApplicationService,
) {

    @PostMapping
    @Operation(summary = "주최자 권한 신청", description = "참가자가 주최자 권한을 신청합니다.")
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
            .body(ApiResponse.created("주최자 권한신청 성공",response))
    }
}