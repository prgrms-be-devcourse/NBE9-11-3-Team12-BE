package com.rungo.api.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        // 기본 설정 SimpleAsyncTaskExecutor -> Thread 무제한 생성 가능해 OOM 발생 가능
        // 직접 설정하여 OOM 방지(MVP 단계로 최소한으로 생성)
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 항상 대기하는 인원
        executor.setMaxPoolSize(5); // 대기열 꽉 차면 추가로 투입하는 인원
        executor.setQueueCapacity(50); // 작업 대기공간
        executor.setThreadNamePrefix("Mail-Async-"); // 로그 확인용 접두사
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return mailExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncExceptionHandler();
    }

    static class LoggingAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error(
                    "비동기 작업 예외 발생: method={}, params={}",
                    method.getName(),
                    Arrays.toString(params),
                    ex
            );
        }
    }
}