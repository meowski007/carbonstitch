package com.carbonstitch.footprint.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Value("${app.async.core-pool-size:4}")
  private int corePoolSize;

  @Value("${app.async.max-pool-size:12}")
  private int maxPoolSize;

  @Value("${app.async.queue-capacity:100}")
  private int queueCapacity;

  @Value("${app.async.thread-name-prefix:footprint-batch-}")
  private String threadNamePrefix;

  @Bean(name = "calculationTaskExecutor")
  public Executor calculationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix(threadNamePrefix);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
  }
}
