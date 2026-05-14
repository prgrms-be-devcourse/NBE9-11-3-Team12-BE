package com.rungo.api.domain.users.admin.controller;

import com.rungo.api.domain.users.admin.dto.AdminApproveRes;
import com.rungo.api.domain.users.admin.service.AdminService;
import com.rungo.api.global.response.ApiResponse;
import com.rungo.api.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "관리자 API")
@SecurityRequirement(name = "accessTokenCookie")
public class AdminController {

    private final AdminService adminService;

    @PatchMapping("/{userId}/organizer")
    @Operation(summary = "주최자 권한 승인", description = "관리자가 특정 사용자에게 ORGANIZER 권한을 부여합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "승인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 주최자 권한 보유")
    })
    public ResponseEntity<ApiResponse<AdminApproveRes>> approveOrganizer(

            @AuthenticationPrincipal SecurityUser admin,

            @PathVariable @NotNull Long userId

    ) {

        AdminApproveRes adminApproveRes =adminService.approveOrganizer(admin.getId(), userId);

        return ResponseEntity.ok(ApiResponse.ok(adminApproveRes));

    }
}
