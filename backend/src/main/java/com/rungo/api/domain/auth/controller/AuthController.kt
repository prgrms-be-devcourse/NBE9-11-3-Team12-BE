package com.rungo.api.domain.auth.controller

import com.rungo.api.domain.auth.dto.*
import com.rungo.api.domain.auth.service.AuthService
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.util.CookieUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 관련 API")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "사용자 회원가입을 진행합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "201", description = "회원가입 성공"),
        SwaggerResponse(responseCode = "400", description = "입력값 검증 실패"),
        SwaggerResponse(responseCode = "409", description = "중복 이메일")
    )
    fun signup(@Valid @RequestBody req: SignUpReq): ResponseEntity<ApiResponse<SignUpRes>> {
        val res = authService.signup(req)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created("회원가입 성공", res))
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인에 성공하면 accessToken, refreshToken 쿠키를 발급합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "로그인 성공"),
        SwaggerResponse(responseCode = "400", description = "입력값 검증 실패"),
        SwaggerResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    )
    fun login(
        @Valid @RequestBody req: LoginReq,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<LoginRes>> {
        val result = authService.login(req)

        CookieUtil.addCookie(response, "accessToken", result.accessToken, ACCESS_TOKEN_EXPIRE)
        CookieUtil.addCookie(response, "refreshToken", result.refreshToken, REFRESH_TOKEN_EXPIRE)

        return ResponseEntity.ok(ApiResponse.ok(result.loginRes))
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "refreshToken 쿠키와 Redis 저장값을 제거합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "로그아웃 성공"),
        SwaggerResponse(responseCode = "401", description = "유효하지 않은 토큰")
    )
    fun logout(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Void?>> {
        authService.logout(refreshToken, response)
        return ResponseEntity.ok(ApiResponse.okMessage("로그아웃되었습니다."))
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "refreshToken 쿠키를 accessToken과 검증하고 토큰을 재발급합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "토큰 재발급 성공"),
        SwaggerResponse(responseCode = "401", description = "유효하지 않거나 만료된 refreshToken"),
        SwaggerResponse(responseCode = "409", description = "재발급 충돌")
    )
    fun reissue(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Void?>> {
        val tokenRes = authService.tokenReissue(refreshToken)

        CookieUtil.addCookie(response, "accessToken", tokenRes.accessToken, ACCESS_TOKEN_EXPIRE)
        CookieUtil.addCookie(response, "refreshToken", tokenRes.refreshToken, REFRESH_TOKEN_EXPIRE)

        return ResponseEntity.ok(ApiResponse.okMessage("토큰이 재발급되었습니다."))
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자 정보를 조회합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "조회 성공"),
        SwaggerResponse(responseCode = "401", description = "인증되지 않은 사용자")
    )
    fun me(authentication: Authentication?): ResponseEntity<ApiResponse<MeRes>> {
        val email = extractEmail(authentication)
        val res = authService.getMe(email)
        return ResponseEntity.ok(ApiResponse.ok(res))
    }

    private fun extractEmail(authentication: Authentication?): String {
        return when (val principal = authentication?.principal) {
            is OAuth2User -> principal.attributes["email"] as? String
            is UserDetails -> principal.username
            is String -> principal
            else -> null
        } ?: throw CustomException(ErrorCode.UNAUTHORIZED)
    }

    companion object {
        private const val ACCESS_TOKEN_EXPIRE = 60 * 60
        private const val REFRESH_TOKEN_EXPIRE = 60 * 60 * 24 * 7
    }
}