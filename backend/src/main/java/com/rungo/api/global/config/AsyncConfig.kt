package com.rungo.api.global.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
@EnableRetry
class AsyncConfig : AsyncConfigurer {
    @Bean(name = ["mailExecutor"])
    fun mailExecutor(): Executor =
        // 기본 설정 SimpleAsyncTaskExecutor -> Thread 무제한 생성 가능해 OOM 발생 가능
        // 직접 설정하여 OOM 방지(MVP 단계로 최소한으로 생성)
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 2 // 항상 대기하는 인원
            maxPoolSize = 5 // 대기열 꽉 차면 추가로 투입하는 인원
            queueCapacity = 50 // 작업 대기공간
            threadNamePrefix = "Mail-Async-" // 로그 확인용 접두사
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(10)
            initialize()
        }

    override fun getAsyncExecutor(): Executor = mailExecutor()

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler =
        LOGGING_ASYNC_EXCEPTION_HANDLER

    companion object {
        private val log = LoggerFactory.getLogger(AsyncConfig::class.java)

        private val LOGGING_ASYNC_EXCEPTION_HANDLER =
            AsyncUncaughtExceptionHandler { ex, method, params ->
                log.error(
                "비동기 작업 예외 발생: method={}, params={}",
                method.name,
                params.contentToString(),
                ex
            )
            }
    }
}
