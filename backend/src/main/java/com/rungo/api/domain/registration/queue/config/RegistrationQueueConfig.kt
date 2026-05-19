package com.rungo.api.domain.registration.queue.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RegistrationQueueProperties::class)
class RegistrationQueueConfig
