package com.rungo.api.domain.auth.service;

import com.rungo.api.domain.auth.entity.RefreshToken;
import com.rungo.api.domain.auth.repository.RefreshTokenRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // refreshToken 저장
    public void saveRefreshToken(Long userId, String refreshToken) {

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .refreshToken(refreshToken)
                .build();

        refreshTokenRepository.save(token); // 기존 값 자동 덮어쓰기
    }

    // userId로 RefreshToken 조회
    public String getRefreshToken(Long userId) {
        return refreshTokenRepository.findById(userId)
                .map(RefreshToken::getRefreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    // RefreshToken 삭제
    public void deleteRefreshToken(Long userId) {
        refreshTokenRepository.deleteById(userId);
    }
}