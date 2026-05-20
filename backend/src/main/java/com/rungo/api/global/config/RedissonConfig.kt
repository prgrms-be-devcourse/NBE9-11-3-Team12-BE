package com.rungo.api.global.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    @param:Value("\${spring.data.redis.host}")
    private val host: String,
    @param:Value("\${spring.data.redis.port}")
    private val port: Int,
) {
    @Bean
    fun redissonClient(): RedissonClient =
        Redisson.create(
            Config().apply {
                useSingleServer().address = "redis://$host:$port"
            }
        )
}
