package com.rungo.api.domain.auth.service

import com.rungo.api.domain.auth.dto.*
import com.rungo.api.domain.auth.entity.UserAuth
import com.rungo.api.domain.auth.repository.UserAuthRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Provider
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.util.CookieUtil
import com.rungo.api.global.util.JwtUtil
import jakarta.servlet.http.HttpServletResponse
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
@Transactional(readOnly = true)
class AuthService(
    private val redissonClient: RedissonClient,
    private val authTransactionService: AuthTransactionService,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: UserRepository,
    private val userAuthRepository: UserAuthRepository
) {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${lock.reissue.wait-time}")
    private var lockWaitTime: Long = 0

    @Value("\${lock.reissue.lease-time}")
    private var lockLeaseTime: Long = 0

    @Transactional
    fun signup(req: SignUpReq): SignUpRes {
        if (userRepository.findByEmail(req.email).isPresent) {
            throw CustomException(ErrorCode.DUPLICATE_EMAIL)
        }

        val user = Users.builder()
            .email(req.email)
            .name(req.name)
            .phoneNumber(req.phoneNumber)
            .gender(req.gender)
            .birth(req.birth)
            .role(Role.PARTICIPANT) // PARTICIPANT 고정
            .build()

        val savedUser = userRepository.save(user)

        userAuthRepository.save(
            UserAuth.createLocalAuth(savedUser, passwordEncoder.encode(req.password))
        )

        val userId = savedUser.id
            ?: throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)

        val createdAt = savedUser.createdAt

        return SignUpRes(
            userId,
            savedUser.email,
            savedUser.name,
            savedUser.phoneNumber,
            savedUser.gender,
            savedUser.birth,
            savedUser.role,
            createdAt
        )
    }

    @Transactional
    fun login(req: LoginReq): LoginResult {
        val userAuth = userAuthRepository.findByUser_EmailAndProvider(req.email, Provider.LOCAL).orElse(null)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (userAuth.password == null ||
            !passwordEncoder.matches(req.password, userAuth.password)
        ) {
            throw CustomException(ErrorCode.INVALID_CREDENTIALS)
        }

        val user = userAuth.user

        val userId = user.id
            ?: throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)

        val accessToken = JwtUtil.generateAccessToken(
            userId,
            user.email,
            user.role,
            jwtSecret
        )

        val refreshToken = JwtUtil.generateRefreshToken(
            userId,
            user.email,
            jwtSecret
        )

        // Redis에 refreshToken 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken)

        val loginRes = LoginRes(
            userId,
            user.email,
            user.name,
            user.role
        )

        return LoginResult(accessToken, refreshToken, loginRes)
    }

    @Transactional
    fun logout(refreshToken: String?, response: HttpServletResponse) {
        // refreshToken이 있으면 Redis에서 삭제
        if (refreshToken != null && JwtUtil.validateToken(refreshToken, jwtSecret)) {
            val userId = JwtUtil.getUserId(refreshToken, jwtSecret)
            if (userId != null) {
                refreshTokenService.deleteRefreshToken(userId)
            }
        }

        // 쿠키 삭제
        CookieUtil.deleteCookie(response, "accessToken")
        CookieUtil.deleteCookie(response, "refreshToken")
    }

    fun tokenReissue(refreshToken: String?): TokenRes {
        // refreshToken null 체크
        if (refreshToken == null) {
            throw CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
        }

        // 토큰 검증
        if (!JwtUtil.validateToken(refreshToken, jwtSecret)) {
            throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)
        }

        // userId 추출
        val userId = JwtUtil.getUserId(refreshToken, jwtSecret)
            ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        // 유저별 락 키 설정
        val lock = redissonClient.getLock("$REISSUE_LOCK_PREFIX$userId")

        try {
            // 최대 1초 대기, 2초 후 자동 해제
            val acquired = lock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS)

            if (!acquired) {
                throw CustomException(ErrorCode.TOKEN_REISSUE_IN_PROGRESS)
            }

            // 트랜잭션 커밋 완료 후 락 해제
            return authTransactionService.reissueToken(userId, refreshToken)

        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CustomException(ErrorCode.TOKEN_REISSUE_FAILED)
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    fun getMe(email: String): MeRes {
        val user = userRepository.findByEmail(email).orElse(null)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        return MeRes.from(user)
    }

    companion object {
        private const val REISSUE_LOCK_PREFIX = "lock:reissue:"
    }
}