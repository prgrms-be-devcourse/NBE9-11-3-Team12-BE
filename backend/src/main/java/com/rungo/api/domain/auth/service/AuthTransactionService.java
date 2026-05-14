package com.rungo.api.domain.auth.service;

import com.rungo.api.domain.auth.dto.TokenRes;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import com.rungo.api.global.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthTransactionService {

    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    // 락 내부에서 트랜잭션 시작, 커밋 후 락 해제
    public TokenRes reissueToken(Long userId, String refreshToken) {

        // Redis 조회
        String storedRefreshToken = refreshTokenService.getRefreshToken(userId);

        // 탈취 감지 시 Redis 삭제 후 강제 로그아웃
        if (!storedRefreshToken.equals(refreshToken)) {
            refreshTokenService.deleteRefreshToken(userId);
            throw new CustomException(ErrorCode.TOKEN_MISMATCH);
        }

        // 사용자 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // accessToken 재발급
        String newAccessToken = JwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                jwtSecret
        );

        // refreshToken 재발급
        String newRefreshToken = JwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                jwtSecret
        );

        // Redis refreshToken 교체
        refreshTokenService.saveRefreshToken(userId, newRefreshToken);

        return new TokenRes(newAccessToken, newRefreshToken);
    }
}