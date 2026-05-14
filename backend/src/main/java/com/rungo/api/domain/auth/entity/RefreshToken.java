package com.rungo.api.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash(value = "refresh", timeToLive = 60 * 60 * 24 * 7) // Redis Key의 prefix 설정, 7일 (초단위)
public class RefreshToken {

    @Id
    private Long userId; // refreshToken 식별용

    private String refreshToken;
}