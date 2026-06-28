package com.sharenote.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {


    // Executor 1: For quick, critical tasks
    @Bean(name = "criticalTaskExecutor")
    public Executor criticalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(10000);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("Critical-Async-Thread-");
        executor.initialize();
        return executor;
    }
    // Executor 2: For event background tasks
    @Bean(name = "eventTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // Minimum number of threads to keep alive
        executor.setMaxPoolSize(10);      // Maximum threads allowed if queue fills up
        executor.setQueueCapacity(500);   // Number of tasks allowed in queue before expanding pool
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("Event-Async-Thread-");
        executor.initialize();
        return executor;
    }

    // Executor 3: For heavy, slow background tasks
    @Bean(name = "backgroundTaskExecutor")
    public Executor backgroundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000); // Larger queue, fewer threads
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("Background-Async-Thread-");
        executor.initialize();
        return executor;
    }
}
