package com.rungo.api.global.util;

import com.rungo.api.domain.users.enumtype.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    static int ACCESS_TOKEN_EXPIRE = 60 * 60; // 1시간
    static int REFRESH_TOKEN_EXPIRE = 60 * 60 * 24 * 7; // 7일

     // JWT 토큰 생성
    public static String generateToken(String secret, long expireSeconds, Map<String, Object> claims) {

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000L);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

     // Access Token 생성 (id, email, role 포함)
    public static String generateAccessToken(Long id, String email, Role role, String secret) {
        return generateToken(secret, ACCESS_TOKEN_EXPIRE,
                Map.of(
                        "id", id,
                        "email", email,
                        "role", role
                )
        );
    }

     //  Refresh Token 생성 (id, email 포함)
    public static String generateRefreshToken(Long id, String email, String secret) {
        return generateToken(secret, REFRESH_TOKEN_EXPIRE,
                Map.of(
                        "id", id,
                        "email", email
                )
        );
    }

    // JWT 겁증
    public static boolean validateToken(String token, String secret) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parse(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

     // JWT Claims 추출
    public static Claims getClaims(String token, String secret) {

        if (!validateToken(token, secret)) return null;

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

     // 이메일 추출
     public static String getEmail(String token, String secret) {
         Claims claims = getClaims(token, secret);
         return claims != null ? claims.get("email", String.class) : null;
     }

     // 역할 추출
    public static String getRole(String token, String secret) {
        Claims claims = getClaims(token, secret);
        return claims != null ? claims.get("role", String.class) : null;
    }

     // 사용자 ID 추출
    public static Long getUserId(String token, String secret) {
        Claims claims = getClaims(token, secret);
        return claims != null ? claims.get("id", Long.class) : null;
    }
}