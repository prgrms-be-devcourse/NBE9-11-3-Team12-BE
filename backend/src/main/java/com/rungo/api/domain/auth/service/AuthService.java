package com.rungo.api.domain.auth.service;

import com.rungo.api.domain.auth.dto.*;
import com.rungo.api.domain.auth.entity.UserAuth;
import com.rungo.api.domain.auth.repository.UserAuthRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Provider;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import com.rungo.api.global.util.CookieUtil;
import com.rungo.api.global.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final RedissonClient redissonClient;
    private final AuthTransactionService authTransactionService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    private static final String REISSUE_LOCK_PREFIX = "lock:reissue:";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${lock.reissue.wait-time}")
    private long lockWaitTime;

    @Value("${lock.reissue.lease-time}")
    private long lockLeaseTime;

    @Transactional
    public SignUpRes signup(SignUpReq req) {

        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        Users user = Users.builder()
                          .email(req.email())
                          .name(req.name())
                          .phoneNumber(req.phoneNumber())
                          .gender(req.gender())
                          .birth(req.birth())
                          .role(Role.PARTICIPANT) // PARTICIPANT 고정
                          .build();

        Users savedUser = userRepository.save(user);

        userAuthRepository.save(
                UserAuth.createLocalAuth(savedUser, passwordEncoder.encode(req.password()))
        );

        return new SignUpRes(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getPhoneNumber(),
                savedUser.getGender(),
                savedUser.getBirth(),
                savedUser.getRole(),
                savedUser.getCreatedAt()
        );
    }

    @Transactional
    public LoginResult login(LoginReq req) {

        UserAuth userAuth = userAuthRepository.findByUser_EmailAndProvider(req.email(), Provider.LOCAL)
                                              .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (userAuth.getPassword() == null ||
                !passwordEncoder.matches(req.password(), userAuth.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        Users user = userAuth.getUser();

        String accessToken = JwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                jwtSecret
        );

        String refreshToken = JwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                jwtSecret
        );

        refreshTokenService.saveRefreshToken(user.getId(), refreshToken); // Redis에 refreshToken 저장

        LoginRes loginRes = new LoginRes(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );

        return new LoginResult(accessToken, refreshToken, loginRes);
    }

    @Transactional
    public void logout(String refreshToken, HttpServletResponse response) {

        // refreshToken이 있으면 Redis에서 삭제
        if (refreshToken != null && JwtUtil.validateToken(refreshToken, jwtSecret)) {
            Long userId = JwtUtil.getUserId(refreshToken, jwtSecret);
            refreshTokenService.deleteRefreshToken(userId);
        }

        // 쿠키 삭제
        CookieUtil.deleteCookie(response, "accessToken");
        CookieUtil.deleteCookie(response, "refreshToken");
    }

    public TokenRes tokenReissue(String refreshToken) {

        // refreshToken null 체크
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 토큰 검증
        if (!JwtUtil.validateToken(refreshToken, jwtSecret)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // userId 추출
        Long userId = JwtUtil.getUserId(refreshToken, jwtSecret);

        // 유저별 락 키 설정
        RLock lock = redissonClient.getLock(REISSUE_LOCK_PREFIX + userId);

        try {
            // 최대 1초 대기, 2초 후 자동 해제
            boolean acquired = lock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS);

            if (!acquired) {
                throw new CustomException(ErrorCode.TOKEN_REISSUE_IN_PROGRESS);
            }

            // 트랜잭션 커밋 완료 후 락 해제
            return authTransactionService.reissueToken(userId, refreshToken);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.TOKEN_REISSUE_FAILED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public MeRes getMe(String email) {

        Users user = userRepository.findByEmail(email)
                                   .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return MeRes.from(user);
    }
}