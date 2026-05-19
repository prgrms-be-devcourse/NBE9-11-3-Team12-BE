package com.rungo.api.domain.registration.queue.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RegistrationQueueProperties::class)
class RegistrationQueueConfig
