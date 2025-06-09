package com.capstone.rentit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("mqttProcessorExecutor")
    public ThreadPoolTaskExecutor mqttProcessorExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(20);       // 평상시 20개 스레드
        exec.setMaxPoolSize(50);        // 필요시 최대 50개
        exec.setQueueCapacity(500);     // 최대 500개까지 대기
        exec.setThreadNamePrefix("mqtt-async-");
        exec.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        exec.initialize();
        return exec;
    }
}
