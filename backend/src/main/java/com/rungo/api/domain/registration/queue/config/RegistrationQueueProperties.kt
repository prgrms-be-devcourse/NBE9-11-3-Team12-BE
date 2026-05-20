package com.rungo.api.domain.registration.queue.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "registration.queue")
data class RegistrationQueueProperties(
    val schedulerEnabled: Boolean,
    val pollInterval: Long,
    val workerConcurrency: Int,
    val courseQueueMaxSize: Int,
    val processingTtl: Long,
    val payloadTtl: Long,
    val resultTtl: Long
)
