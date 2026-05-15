package com.rungo.api.domain.auth.service;

import com.rungo.api.domain.auth.dto.*;
import com.rungo.api.domain.auth.entity.UserAuth;
import com.rungo.api.domain.auth.repository.UserAuthRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Provider;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import com.rungo.api.global.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private AuthTransactionService authTransactionService;

    @Mock
    private RLock lock;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtSecret", "test-secret-key-at-least-32-bytes-long");
        ReflectionTestUtils.setField(authService, "lockWaitTime", 1L);
        ReflectionTestUtils.setField(authService, "lockLeaseTime", 2L);
    }

    // 회원가입 테스트
    @Test
    @DisplayName("회원가입 성공 - 유효한 정보 입력 시 비밀번호가 암호화되어 저장되고 결과를 반환한다")
    void signup_success() {
        SignUpReq req = new SignUpReq(
                "test@test.com", "pass123!", "홍길동", "010-1234-5678",
                Gender.MALE, LocalDate.of(2000, 1, 1)
        );

        Users savedUser = Users.builder()
                .id(1L)
                .email(req.email())
                .name(req.name())
                .phoneNumber(req.phoneNumber())
                .gender(req.gender())
                .birth(req.birth())
                .role(Role.PARTICIPANT)
                .build();

        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encoded-pass");
        given(userRepository.save(any(Users.class))).willReturn(savedUser);

        SignUpRes res = authService.signup(req);

        assertNotNull(res);
        assertEquals(1L, res.id());
        assertEquals("test@test.com", res.email());
        assertEquals("홍길동", res.name());
        assertEquals(Role.PARTICIPANT, res.role());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일이 중복되면 DUPLICATE_EMAIL 예외가 발생한다")
    void signup_fail_duplicate_email() {
        SignUpReq req = new SignUpReq(
                "duplicate@test.com", "pass123!", "홍길동", "010-1111-2222",
                Gender.MALE, LocalDate.of(2000, 1, 1)
        );

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(Users.builder().build()));

        CustomException exception = assertThrows(CustomException.class, () -> authService.signup(req));
        assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.getErrorCode());
    }

    // 로그인 테스트
    @Test
    @DisplayName("로그인 성공 - 유효한 정보 입력 시 토큰과 유저 정보를 반환한다")
    void login_success() {
        LoginReq req = new LoginReq("test@test.com", "pass123!");

        Users user = Users.builder()
                .id(1L)
                .email("test@test.com")
                .name("홍길동")
                .role(Role.PARTICIPANT)
                .build();
        UserAuth userAuth = UserAuth.createLocalAuth(user, "encoded-pass");

        given(userAuthRepository.findByUser_EmailAndProvider(req.email(), Provider.LOCAL))
                .willReturn(Optional.of(userAuth));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        LoginResult result = authService.login(req);

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        assertEquals(1L, result.loginRes().userId());
        assertEquals("test@test.com", result.loginRes().email());
        assertEquals("홍길동", result.loginRes().name());

        // Redis에 refreshToken 저장 호출 검증
        then(refreshTokenService).should().saveRefreshToken(eq(1L), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일이면 USER_NOT_FOUND 예외가 발생한다")
    void login_fail_user_not_found() {
        LoginReq req = new LoginReq("notfound@test.com", "pass123!");

        given(userAuthRepository.findByUser_EmailAndProvider(req.email(), Provider.LOCAL))
                .willReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () -> authService.login(req));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호가 일치하지 않으면 INVALID_CREDENTIALS 예외가 발생한다")
    void login_fail_password_mismatch() {
        LoginReq req = new LoginReq("test@test.com", "wrong-pass");

        Users user = Users.builder()
                .email("test@test.com")
                .build();
        UserAuth userAuth = UserAuth.createLocalAuth(user, "encoded-pass");

        given(userAuthRepository.findByUser_EmailAndProvider(req.email(), Provider.LOCAL))
                .willReturn(Optional.of(userAuth));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> authService.login(req));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    // 로그아웃 테스트
    @Test
    @DisplayName("로그아웃 성공 - 유효한 refreshToken이면 Redis에서 삭제하고 쿠키를 제거한다")
    void logout_success_with_valid_token() {
        // 실제 유효한 토큰 생성
        String refreshToken = JwtUtil.generateRefreshToken(1L, "test@test.com",
                "test-secret-key-at-least-32-bytes-long");

        HttpServletResponse response = mock(HttpServletResponse.class);

        authService.logout(refreshToken, response);

        // Redis 삭제 호출 검증
        then(refreshTokenService).should().deleteRefreshToken(1L);
    }

    @Test
    @DisplayName("로그아웃 성공 - refreshToken이 null이면 Redis 삭제 없이 쿠키만 제거한다")
    void logout_success_with_null_token() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        authService.logout(null, response);

        // Redis 삭제가 호출되지 않아야 함
        then(refreshTokenService).should(never()).deleteRefreshToken(any());
    }

    @Test
    @DisplayName("로그아웃 성공 - refreshToken이 유효하지 않으면 Redis 삭제 없이 쿠키만 제거한다")
    void logout_success_with_invalid_token() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        authService.logout("invalid-token", response);

        // Redis 삭제가 호출되지 않아야 함
        then(refreshTokenService).should(never()).deleteRefreshToken(any());
    }

    // 토큰 재발급 테스트
    @Test
    @DisplayName("토큰 재발급 성공 - 유효한 refreshToken이면 새 토큰 2개를 반환하고 Redis가 갱신된다")
    void tokenReissue_success() throws InterruptedException {
        String refreshToken = JwtUtil.generateRefreshToken(1L, "test@test.com",
                "test-secret-key-at-least-32-bytes-long");

        givenTokenReissueLockAcquired();
        given(authTransactionService.reissueToken(1L, refreshToken))
                .willReturn(new TokenRes("access-token", "refresh-token"));

        TokenRes result = authService.tokenReissue(refreshToken);

        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        then(authTransactionService).should().reissueToken(1L, refreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - refreshToken이 null이면 REFRESH_TOKEN_NOT_FOUND 예외가 발생한다")
    void tokenReissue_fail_null_token() {
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.tokenReissue(null));

        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 refreshToken이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
    void tokenReissue_fail_invalid_token() {
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.tokenReissue("invalid-token"));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis 저장 토큰과 불일치하면 TOKEN_MISMATCH 예외가 발생하고 Redis가 삭제된다")
    void tokenReissue_fail_token_mismatch() throws InterruptedException {
        String refreshToken = JwtUtil.generateRefreshToken(1L, "test@test.com",
                "test-secret-key-at-least-32-bytes-long");

        givenTokenReissueLockAcquired();
        given(authTransactionService.reissueToken(1L, refreshToken))
                .willThrow(new CustomException(ErrorCode.TOKEN_MISMATCH));

        CustomException exception = assertThrows(CustomException.class,
                () -> authService.tokenReissue(refreshToken));

        assertEquals(ErrorCode.TOKEN_MISMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - userId로 유저 조회 실패 시 USER_NOT_FOUND 예외가 발생한다")
    void tokenReissue_fail_user_not_found() throws InterruptedException {
        String refreshToken = JwtUtil.generateRefreshToken(1L, "test@test.com",
                "test-secret-key-at-least-32-bytes-long");

        givenTokenReissueLockAcquired();
        given(authTransactionService.reissueToken(1L, refreshToken))
                .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

        CustomException exception = assertThrows(CustomException.class,
                () -> authService.tokenReissue(refreshToken));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    private void givenTokenReissueLockAcquired() throws InterruptedException {
        given(redissonClient.getLock("lock:reissue:1")).willReturn(lock);
        given(lock.tryLock(1L, 2L, TimeUnit.SECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
    }
}
