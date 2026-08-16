package com.multidrive.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebMvcAsyncConfig implements WebMvcConfigurer {

	private final AsyncTaskExecutor mvcStreamingTaskExecutor;

	public WebMvcAsyncConfig(

			@Qualifier("mvcStreamingTaskExecutor") AsyncTaskExecutor mvcStreamingTaskExecutor) {

		this.mvcStreamingTaskExecutor = mvcStreamingTaskExecutor;
	}

	@Bean(name = "mvcStreamingTaskExecutor")
	public static AsyncTaskExecutor mvcStreamingTaskExecutor() {

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(4);

		executor.setMaxPoolSize(16);

		executor.setQueueCapacity(100);

		executor.setThreadNamePrefix("mvc-stream-");

		executor.setWaitForTasksToCompleteOnShutdown(true);

		executor.setAwaitTerminationSeconds(30);

		executor.initialize();

		return executor;
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {

		configurer.setTaskExecutor(mvcStreamingTaskExecutor);

		configurer.setDefaultTimeout(Duration.ofHours(2).toMillis());
	}

}
