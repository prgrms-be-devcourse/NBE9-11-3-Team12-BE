package com.rungo.api.domain.auth.service

import com.rungo.api.domain.auth.entity.RefreshToken
import com.rungo.api.domain.auth.repository.RefreshTokenRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    // refreshToken 저장
    fun saveRefreshToken(userId: Long, refreshToken: String) {
        val token = RefreshToken.builder()
            .userId(userId)
            .refreshToken(refreshToken)
            .build()

        refreshTokenRepository.save(token)
    }

    // userId로 RefreshToken 조회
    fun getRefreshToken(userId: Long): String =
        refreshTokenRepository.findByIdOrNull(userId)?.refreshToken
            ?: throw CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)

    // RefreshToken 삭제
    fun deleteRefreshToken(userId: Long) {
        refreshTokenRepository.deleteById(userId)
    }
}