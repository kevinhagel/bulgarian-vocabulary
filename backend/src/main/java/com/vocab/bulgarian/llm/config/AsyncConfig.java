package com.vocab.bulgarian.llm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for asynchronous LLM execution using Java 25 virtual threads.
 * LLM calls are I/O-bound (blocking HTTP to Ollama) — virtual threads are ideal:
 * no thread pool size limits, no OS thread blocking, minimal overhead.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "llmTaskExecutor")
    public Executor llmTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "audioTaskExecutor")
    public Executor audioTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public Executor getAsyncExecutor() {
        return llmTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
            logger.error("Uncaught async exception in method {}: {}",
                method.getName(), throwable.getMessage(), throwable);
    }
}
