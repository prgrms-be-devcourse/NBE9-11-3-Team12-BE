package com.rungo.api.domain.auth.service

import com.rungo.api.domain.auth.dto.TokenRes
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import com.rungo.api.global.util.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthTransactionService(
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: UserRepository
) {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    // 락 내부에서 트랜잭션 시작, 커밋 후 락 해제
    fun reissueToken(userId: Long, refreshToken: String): TokenRes {

        // Redis 조회
        val storedRefreshToken = refreshTokenService.getRefreshToken(userId)

        // 탈취 감지 시 Redis 삭제 후 강제 로그아웃
        if (storedRefreshToken != refreshToken) {
            refreshTokenService.deleteRefreshToken(userId)
            throw CustomException(ErrorCode.TOKEN_MISMATCH)
        }

        // 사용자 조회
        val user = userRepository.findByIdOrNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        // accessToken 재발급
        val newAccessToken = JwtUtil.generateAccessToken(
            userId,
            user.email,
            user.role,
            jwtSecret
        )

        // refreshToken 재발급
        val newRefreshToken = JwtUtil.generateRefreshToken(
            userId,
            user.email,
            jwtSecret
        )

        // Redis refreshToken 교체
        refreshTokenService.saveRefreshToken(userId, newRefreshToken)

        return TokenRes(newAccessToken, newRefreshToken)
    }
}