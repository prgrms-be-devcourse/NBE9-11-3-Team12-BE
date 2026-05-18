package com.rungo.api.domain.auth.service

import com.rungo.api.domain.auth.dto.LoginReq
import com.rungo.api.domain.auth.dto.SignUpReq
import com.rungo.api.domain.auth.dto.TokenRes
import com.rungo.api.domain.auth.entity.UserAuth
import com.rungo.api.domain.auth.entity.UserAuth.Companion.createLocalAuth
import com.rungo.api.domain.auth.repository.UserAuthRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.entity.Users.Companion.create
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Provider
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.util.JwtUtil.generateRefreshToken
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
internal class AuthServiceTest {
    @InjectMocks
    private val authService: AuthService? = null

    @Mock
    private val userRepository: UserRepository? = null

    @Mock
    private val passwordEncoder: PasswordEncoder? = null

    @Mock
    private val refreshTokenService: RefreshTokenService? = null

    @Mock
    private val userAuthRepository: UserAuthRepository? = null

    @Mock
    private val redissonClient: RedissonClient? = null

    @Mock
    private val authTransactionService: AuthTransactionService? = null

    @Mock
    private val lock: RLock? = null

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(authService, "jwtSecret", "test-secret-key-at-least-32-bytes-long")
        ReflectionTestUtils.setField(authService, "lockWaitTime", 1L)
        ReflectionTestUtils.setField(authService, "lockLeaseTime", 2L)
    }

    // 회원가입 테스트
    @Test
    @DisplayName("회원가입 성공 - 유효한 정보 입력 시 비밀번호가 암호화되어 저장되고 결과를 반환한다")
    fun signup_success() {
        val req = SignUpReq(
            "test@test.com", "pass123!", "홍길동", "010-1234-5678",
            Gender.MALE, LocalDate.of(2000, 1, 1)
        )

        val savedUser = create(
            req.email,
            req.name,
            req.phoneNumber,
            req.gender,
            req.birth
        )

        ReflectionTestUtils.setField(savedUser, "id", 1L)
        ReflectionTestUtils.setField(savedUser, "createdAt", LocalDateTime.now())

        BDDMockito.given<Users?>(userRepository!!.findByEmail(ArgumentMatchers.anyString())).willReturn(null)
        BDDMockito.given<String?>(passwordEncoder!!.encode(ArgumentMatchers.anyString())).willReturn("encoded-pass")
        BDDMockito.given<Users?>(userRepository.save<Users?>(ArgumentMatchers.any<Users?>(Users::class.java)))
            .willReturn(savedUser)

        val res = authService!!.signup(req)

        Assertions.assertNotNull(res)
        Assertions.assertEquals(1L, res.id)
        Assertions.assertEquals("test@test.com", res.email)
        Assertions.assertEquals("홍길동", res.name)
        Assertions.assertEquals(Role.PARTICIPANT, res.role)
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일이 중복되면 DUPLICATE_EMAIL 예외가 발생한다")
    fun signup_fail_duplicate_email() {
        val req = SignUpReq(
            "duplicate@test.com", "pass123!", "홍길동", "010-1111-2222",
            Gender.MALE, LocalDate.of(2000, 1, 1)
        )

        val existingUser = create(
            req.email,
            req.name,
            req.phoneNumber,
            req.gender,
            req.birth
        )

        BDDMockito.given<Users?>(userRepository!!.findByEmail(ArgumentMatchers.anyString())).willReturn(existingUser)

        val exception = Assertions.assertThrows<CustomException>(
            CustomException::class.java,
            Executable { authService!!.signup(req) })
        Assertions.assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.errorCode)
    }

    // 로그인 테스트
    @Test
    @DisplayName("로그인 성공 - 유효한 정보 입력 시 토큰과 유저 정보를 반환한다")
    fun login_success() {
        val req = LoginReq("test@test.com", "pass123!")

        val user = create(
            "test@test.com",
            "홍길동",
            "010-1234-5678",
            Gender.MALE,
            LocalDate.of(2000, 1, 1)
        )

        ReflectionTestUtils.setField(user, "id", 1L)

        val userAuth = createLocalAuth(
            user,
            "encoded-pass"
        )

        BDDMockito.given<UserAuth?>(
            userAuthRepository!!.findByUser_EmailAndProvider(
                req.email,
                Provider.LOCAL
            )
        ).willReturn(userAuth)

        BDDMockito.given<Boolean?>(
            passwordEncoder!!.matches(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).willReturn(true)

        val result = authService!!.login(req)

        Assertions.assertNotNull(result)
        Assertions.assertNotNull(result.accessToken)
        Assertions.assertNotNull(result.refreshToken)
        Assertions.assertEquals(1L, result.loginRes.userId)
        Assertions.assertEquals("test@test.com", result.loginRes.email)
        Assertions.assertEquals("홍길동", result.loginRes.name)

        // Redis에 refreshToken 저장 호출 검증
        BDDMockito.then<RefreshTokenService?>(refreshTokenService).should()
            .saveRefreshToken(ArgumentMatchers.eq(1L), ArgumentMatchers.anyString())
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일이면 USER_NOT_FOUND 예외가 발생한다")
    fun login_fail_user_not_found() {
        val req = LoginReq("notfound@test.com", "pass123!")

        BDDMockito.given<UserAuth?>(userAuthRepository!!.findByUser_EmailAndProvider(req.email, Provider.LOCAL))
            .willReturn(null)

        val exception = Assertions.assertThrows<CustomException>(
            CustomException::class.java,
            Executable { authService!!.login(req) })
        Assertions.assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호가 일치하지 않으면 INVALID_CREDENTIALS 예외가 발생한다")
    fun login_fail_password_mismatch() {
        val req = LoginReq("test@test.com", "wrong-pass")

        val user = create(
            "test@test.com",
            "홍길동",
            "010-1234-5678",
            Gender.MALE,
            LocalDate.of(2000, 1, 1)
        )
        val userAuth = createLocalAuth(user, "encoded-pass")

        BDDMockito.given<UserAuth?>(userAuthRepository!!.findByUser_EmailAndProvider(req.email, Provider.LOCAL))
            .willReturn(userAuth)
        BDDMockito.given<Boolean?>(
            passwordEncoder!!.matches(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).willReturn(false)

        val exception = Assertions.assertThrows<CustomException>(
            CustomException::class.java,
            Executable { authService!!.login(req) })
        Assertions.assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
    }

    // 로그아웃 테스트
    @Test
    @DisplayName("로그아웃 성공 - 유효한 refreshToken이면 Redis에서 삭제하고 쿠키를 제거한다")
    fun logout_success_with_valid_token() {
        // 실제 유효한 토큰 생성
        val refreshToken = generateRefreshToken(
            1L, "test@test.com",
            "test-secret-key-at-least-32-bytes-long"
        )

        val response = Mockito.mock<HttpServletResponse>(HttpServletResponse::class.java)

        authService!!.logout(refreshToken, response)

        // Redis 삭제 호출 검증
        BDDMockito.then<RefreshTokenService?>(refreshTokenService).should().deleteRefreshToken(1L)
    }

    @Test
    @DisplayName("로그아웃 성공 - refreshToken이 null이면 Redis 삭제 없이 쿠키만 제거한다")
    fun logout_success_with_null_token() {
        val response = Mockito.mock<HttpServletResponse>(HttpServletResponse::class.java)

        authService!!.logout(null, response)

        // Redis 삭제가 호출되지 않아야 함
        BDDMockito.then<RefreshTokenService?>(refreshTokenService).should(Mockito.never())
            .deleteRefreshToken(ArgumentMatchers.anyLong())
    }

    @Test
    @DisplayName("로그아웃 성공 - refreshToken이 유효하지 않으면 Redis 삭제 없이 쿠키만 제거한다")
    fun logout_success_with_invalid_token() {
        val response = Mockito.mock<HttpServletResponse>(HttpServletResponse::class.java)

        authService!!.logout("invalid-token", response)

        // Redis 삭제가 호출되지 않아야 함
        BDDMockito.then<RefreshTokenService?>(refreshTokenService).should(Mockito.never())
            .deleteRefreshToken(ArgumentMatchers.anyLong())
    }

    // 토큰 재발급 테스트
    @Test
    @DisplayName("토큰 재발급 성공 - 유효한 refreshToken이면 새 토큰 2개를 반환하고 Redis가 갱신된다")
    @Throws(InterruptedException::class)
    fun tokenReissue_success() {
        val refreshToken = generateRefreshToken(
            1L, "test@test.com",
            "test-secret-key-at-least-32-bytes-long"
        )

        givenTokenReissueLockAcquired()
        BDDMockito.given<TokenRes>(authTransactionService!!.reissueToken(1L, refreshToken))
            .willReturn(TokenRes("access-token", "refresh-token"))

        val result = authService!!.tokenReissue(refreshToken)

        Assertions.assertNotNull(result.accessToken)
        Assertions.assertNotNull(result.refreshToken)
        BDDMockito.then<AuthTransactionService?>(authTransactionService).should().reissueToken(1L, refreshToken)
    }

    @Test
    @DisplayName("토큰 재발급 실패 - refreshToken이 null이면 REFRESH_TOKEN_NOT_FOUND 예외가 발생한다")
    fun tokenReissue_fail_null_token() {
        val exception = Assertions.assertThrows<CustomException>(
            CustomException::class.java,
            Executable { authService!!.tokenReissue(null) })

        Assertions.assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.errorCode)
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 refreshToken이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
    fun tokenReissue_fail_invalid_token() {
        val exception = Assertions.assertThrows<CustomException>(
            CustomException::class.java,
            Executable { authService!!.tokenReissue("invalid-token") })

        Assertions.assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.errorCode)
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis 저장 토큰과 불일치하면 TOKEN_MISMATCH 예외가 발생하고 Redis가 삭제된다")
    @Throws(
        InterruptedException::class
    )
    fun tokenReissue_fail_token_mismatch() {
        val refreshToken = generateRefreshToken(
            1L, "test@test.com",
            "test-secret-key-at-least-32-bytes-long"
        )

        givenTokenReissueLockAcquired()
        BDDMockito.given<TokenRes>(authTransactionService!!.reissueToken(1L, refreshToken))
            .willThrow(CustomException(ErrorCode.TOKEN_MISMATCH))

        val exception = Assertions.assertThrows<CustomException>(
            CustomException::class.java,
            Executable { authService!!.tokenReissue(refreshToken) })

        Assertions.assertEquals(ErrorCode.TOKEN_MISMATCH, exception.errorCode)
    }

    @Test
    @DisplayName("토큰 재발급 실패 - userId로 유저 조회 실패 시 USER_NOT_FOUND 예외가 발생한다")
    @Throws(InterruptedException::class)
    fun tokenReissue_fail_user_not_found() {
        val refreshToken = generateRefreshToken(
            1L, "test@test.com",
            "test-secret-key-at-least-32-bytes-long"
        )

        givenTokenReissueLockAcquired()
        BDDMockito.given<TokenRes>(authTransactionService!!.reissueToken(1L, refreshToken))
            .willThrow(CustomException(ErrorCode.USER_NOT_FOUND))

        val exception = Assertions.assertThrows<CustomException>(
            CustomException::class.java,
            Executable { authService!!.tokenReissue(refreshToken) })

        Assertions.assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Throws(InterruptedException::class)
    private fun givenTokenReissueLockAcquired() {
        BDDMockito.given<RLock?>(redissonClient!!.getLock("lock:reissue:1")).willReturn(lock)
        BDDMockito.given<Boolean?>(lock!!.tryLock(1L, 2L, TimeUnit.SECONDS)).willReturn(true)
        BDDMockito.given<Boolean?>(lock.isHeldByCurrentThread()).willReturn(true)
    }
}
