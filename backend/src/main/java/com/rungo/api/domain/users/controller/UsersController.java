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
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "사용자 API")
@SecurityRequirement(name = "accessTokenCookie")
class UsersController {
    private val userService: UsersService? = null

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필을 조회합니다.")
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    )
    fun getMyInfo(@AuthenticationPrincipal user: SecurityUser): ResponseEntity<ApiResponse<MyProfileRes?>?> {        // 인증이 실패된 객체라면
        if (user == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }
        return ResponseEntity.ok<ApiResponse<MyProfileRes?>?>(ApiResponse.ok<MyProfileRes?>(userService!!.getMyInfo(user.getId())))
    }

    @PatchMapping("/me")
    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 이름, 전화번호를 수정합니다.")
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    )
    fun updateMyProfile(
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @RequestBody req: @Valid UpdateMyProfileReq
    ): ResponseEntity<ApiResponse<UpdateMyProfileRes?>?> {
        if (user == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        // 모든 필드가 null이면 의미 없는 요청
        if (req.name == null && req.phoneNumber() == null && req.gender() == null && req.birth() == null) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        val res = userService!!.updateMyProfile(user.getId(), req)
        return ResponseEntity.ok<ApiResponse<UpdateMyProfileRes?>?>(ApiResponse.ok<UpdateMyProfileRes?>(res))
    }

    @PatchMapping("/me/complete")
    @Operation(summary = "내 정보 보완", description = "소셜 로그인 사용자의 추가 정보를 처음 입력합니다.")
    fun completeMyProfile(
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @RequestBody req: @Valid CompleteProfileReq
    ): ResponseEntity<ApiResponse<Void?>?> {
        if (user == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        userService!!.completeMyProfile(user.getId(), req)
        return ResponseEntity.ok<ApiResponse<Void?>?>(ApiResponse.okMessage("프로필이 보완되었습니다."))
    }
}