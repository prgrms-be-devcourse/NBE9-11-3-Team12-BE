package com.rungo.api.domain.users.controller

import com.rungo.api.domain.users.dto.CompleteProfileReq
import com.rungo.api.domain.users.dto.MyProfileRes
import com.rungo.api.domain.users.dto.UpdateMyProfileReq
import com.rungo.api.domain.users.dto.UpdateMyProfileRes
import com.rungo.api.domain.users.service.UsersService
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "사용자 API")
@SecurityRequirement(name = "accessTokenCookie")
class UsersController(
    private val userService: UsersService
) {

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필을 조회합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "조회 성공"),
        SwaggerResponse(responseCode = "401", description = "인증 필요")
    )
    fun getMyInfo(
        @AuthenticationPrincipal user: SecurityUser
    ): ResponseEntity<ApiResponse<MyProfileRes>> {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyInfo(user.id)))
    }

    @PatchMapping("/me")
    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 이름, 전화번호를 수정합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "수정 성공"),
        SwaggerResponse(responseCode = "400", description = "입력값 검증 실패"),
        SwaggerResponse(responseCode = "401", description = "인증 필요")
    )
    fun updateMyProfile(
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @RequestBody req: UpdateMyProfileReq
    ): ResponseEntity<ApiResponse<UpdateMyProfileRes>> {
        if (req.name == null && req.phoneNumber == null) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.updateMyProfile(user.id, req)))
    }

    @PatchMapping("/me/complete")
    @Operation(summary = "내 정보 보완", description = "소셜 로그인 사용자의 추가 정보를 처음 입력합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "보완 성공"),
        SwaggerResponse(responseCode = "400", description = "입력값 검증 실패"),
        SwaggerResponse(responseCode = "401", description = "인증 필요")
    )
    fun completeMyProfile(
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @RequestBody req: CompleteProfileReq
    ): ResponseEntity<ApiResponse<Void?>> {
        userService.completeMyProfile(user.id, req)
        return ResponseEntity.ok(ApiResponse.okMessage("프로필이 보완되었습니다."))
    }
}