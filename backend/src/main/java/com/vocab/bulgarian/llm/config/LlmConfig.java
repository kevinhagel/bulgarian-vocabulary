package com.vocab.bulgarian.llm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AI ChatClient beans.
 * Two beans: one for BgGPT (inflections/detection), one for Qwen 2.5 14B (sentence generation).
 */
@Configuration
public class LlmConfig {

    /**
     * Primary ChatClient: BgGPT for Bulgarian language processing.
     * Used for lemma detection, inflection generation, and metadata classification.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem("You are a Bulgarian language expert. Respond ONLY in valid JSON matching the requested format. Do not include explanations outside the JSON structure.")
            .build();
    }

    /**
     * Secondary ChatClient: Qwen 2.5 14B for example sentence generation.
     * Uses higher temperature (0.7) for natural sentence variety.
     * num-ctx 4096 gives the model enough context for high-quality output.
     */
    @Bean
    @Qualifier("sentenceChatClient")
    public ChatClient sentenceChatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem("You are a Bulgarian language teacher generating example sentences. Respond ONLY in valid JSON matching the requested format.")
            .defaultOptions(OllamaOptions.builder()
                .model("qwen2.5:14b")
                .temperature(0.7)
                .numCtx(4096)
                .numGPU(99)
                .build())
            .build();
    }
}
