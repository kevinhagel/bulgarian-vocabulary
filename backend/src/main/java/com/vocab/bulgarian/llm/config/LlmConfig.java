package com.vocab.bulgarian.llm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AI ChatClient.
 * Builds ChatClient from auto-configured builder with Bulgarian language expert system message.
 */
@Configuration
public class LlmConfig {

    /**
     * ChatClient configured for Bulgarian language processing.
     * System message instructs the LLM to respond in valid JSON matching requested formats.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem("You are a Bulgarian language expert. Respond ONLY in valid JSON matching the requested format. Do not include explanations outside the JSON structure.")
            .build();
    }
}
