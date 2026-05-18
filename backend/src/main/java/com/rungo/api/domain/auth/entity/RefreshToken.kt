package com.rungo.api.domain.auth.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash

@RedisHash(value = "refresh", timeToLive = 60 * 60 * 24 * 7)
class RefreshToken(
    @Id
    val userId: Long,
    val refreshToken: String
)