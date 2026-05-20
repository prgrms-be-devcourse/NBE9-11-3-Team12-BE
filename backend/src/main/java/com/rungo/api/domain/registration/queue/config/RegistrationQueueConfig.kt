package com.rungo.api.domain.registration.queue.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RegistrationQueueProperties::class)
class RegistrationQueueConfig {

    @Bean(name = [REGISTRATION_QUEUE_WORKER_EXECUTOR])
    fun registrationQueueWorkerExecutor(properties: RegistrationQueueProperties): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = properties.workerConcurrency
            maxPoolSize = properties.workerConcurrency
            queueCapacity = properties.workerConcurrency
            threadNamePrefix = "Registration-Queue-Worker-"
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(10)
            initialize()
        }

    companion object {
        const val REGISTRATION_QUEUE_WORKER_EXECUTOR = "registrationQueueWorkerExecutor"
    }
}
