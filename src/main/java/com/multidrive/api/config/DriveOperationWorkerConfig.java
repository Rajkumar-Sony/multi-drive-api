package com.multidrive.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class DriveOperationWorkerConfig {

    @Bean(
            name = "driveOperationTaskExecutor"
    )
    public ThreadPoolTaskExecutor driveOperationTaskExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(
                2
        );

        executor.setMaxPoolSize(
                4
        );

        executor.setQueueCapacity(
                16
        );

        executor.setThreadNamePrefix(
                "drive-op-"
        );

        executor.setWaitForTasksToCompleteOnShutdown(
                true
        );

        executor.setAwaitTerminationSeconds(
                30
        );

        executor.initialize();

        return executor;
    }
}
