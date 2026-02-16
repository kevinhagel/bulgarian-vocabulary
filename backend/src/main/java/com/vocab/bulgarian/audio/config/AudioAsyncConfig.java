package com.vocab.bulgarian.audio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous audio generation execution.
 * Provides a dedicated thread pool separate from LLM operations.
 */
@Configuration
public class AudioAsyncConfig {

    /**
     * Thread pool executor for audio generation tasks.
     * Core pool: 2 threads (I/O-bound, waiting on edge-tts process)
     * Max pool: 4 threads
     * Queue capacity: 50 (larger than LLM since audio is faster)
     * Uses CallerRunsPolicy to apply backpressure when queue is full.
     */
    @Bean(name = "audioTaskExecutor")
    public Executor audioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("audio-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
