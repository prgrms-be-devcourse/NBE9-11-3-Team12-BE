package com.rungo.api.domain.auth.controller;

import com.rungo.api.domain.auth.dto.*;
import com.rungo.api.domain.auth.service.AuthService;
import com.rungo.api.global.response.ApiResponse;
import com.rungo.api.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    private static final int ACCESS_TOKEN_EXPIRE = 60 * 60; // 1시간
    private static final int REFRESH_TOKEN_EXPIRE = 60 * 60 * 24 * 7; // 7일

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "사용자 회원가입을 진행합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 이메일")
    })
    public ResponseEntity<ApiResponse<SignUpRes>> signup(@Valid @RequestBody SignUpReq req) {

        SignUpRes res = authService.signup(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입 성공", res));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인에 성공하면 accessToken, refreshToken 쿠키를 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    public ResponseEntity<ApiResponse<LoginRes>> login(
            @Valid @RequestBody LoginReq req,
            HttpServletResponse response
    ) {
        LoginResult result = authService.login(req);

        CookieUtil.addCookie(response, "accessToken", result.accessToken(), ACCESS_TOKEN_EXPIRE);
        CookieUtil.addCookie(response, "refreshToken", result.refreshToken(), REFRESH_TOKEN_EXPIRE);

        return ResponseEntity.ok(ApiResponse.ok(result.loginRes()));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "refreshToken 쿠키와 Redis 저장값을 제거합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.okMessage("로그아웃되었습니다."));
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "refreshToken 쿠키를 accessToken과 검증하고 토큰을 재발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 refreshToken"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "재발급 충돌")
    })
    public ResponseEntity<ApiResponse<Void>> reissue(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        TokenRes tokenRes = authService.tokenReissue(refreshToken);

        CookieUtil.addCookie(response, "accessToken", tokenRes.accessToken(), ACCESS_TOKEN_EXPIRE);
        CookieUtil.addCookie(response, "refreshToken", tokenRes.refreshToken(), REFRESH_TOKEN_EXPIRE);

        return ResponseEntity.ok(ApiResponse.okMessage("토큰이 재발급되었습니다."));
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<MeRes>> me(Authentication authentication) {
        String email = extractEmail(authentication);
        MeRes res = authService.getMe(email);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oAuth2User) {
            return (String) oAuth2User.getAttributes().get("email");
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String str) {
            return str;
        }

        throw new IllegalArgumentException("지원하지 않는 인증 타입");
    }
}