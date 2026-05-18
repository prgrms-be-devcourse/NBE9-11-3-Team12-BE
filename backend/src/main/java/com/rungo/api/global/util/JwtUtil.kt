package com.rungo.api.global.util

import com.rungo.api.domain.users.enumtype.Role
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.*

object JwtUtil {

    private const val ACCESS_TOKEN_EXPIRE = 60 * 60          // 1시간
    private const val REFRESH_TOKEN_EXPIRE = 60 * 60 * 24 * 7 // 7일

    // JWT 토큰 생성
    fun generateToken(secret: String, expireSeconds: Long, claims: Map<String, Any>): String {
        val now = System.currentTimeMillis()
        val key = Keys.hmacShaKeyFor(secret.toByteArray())

        return Jwts.builder()
            .claims(claims)
            .issuedAt(Date(now))
            .expiration(Date(now + expireSeconds * 1000L))
            .signWith(key)
            .compact()
    }

    // Access Token 생성 (id, email, role 포함)
    fun generateAccessToken(id: Long, email: String, role: Role, secret: String): String =
        generateToken(
            secret,
            ACCESS_TOKEN_EXPIRE.toLong(),
            mapOf("id" to id, "email" to email, "role" to role.name)
        )

    // Refresh Token 생성 (id, email 포함)
    fun generateRefreshToken(id: Long, email: String, secret: String): String =
        generateToken(
            secret,
            REFRESH_TOKEN_EXPIRE.toLong(),
            mapOf("id" to id, "email" to email)
        )

    // JWT 검증
    fun validateToken(token: String, secret: String): Boolean =
        try {
            Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.toByteArray()))
                .build()
                .parse(token)
            true
        } catch (_: Exception) {
            false
        }

    // JWT Claims 추출
    fun getClaims(token: String, secret: String): Claims =
        Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secret.toByteArray()))
            .build()
            .parseSignedClaims(token)
            .payload

    // 사용자 ID 추출
    fun getUserId(token: String, secret: String): Long? =
        runCatching { (getClaims(token, secret)["id"] as Number).toLong() }.getOrNull()
}