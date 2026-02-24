package com.vocab.bulgarian.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OllamaConfig {

    /**
     * RestClient for Ollama using SimpleClientHttpRequestFactory (no connection pool).
     *
     * Apache HttpClient 5's default pool has ~5 connections per route with a 180s
     * acquisition timeout. When batch-adding 30+ words, all virtual threads arrive
     * simultaneously and the pool exhausts within 3 minutes.
     *
     * SimpleClientHttpRequestFactory opens a connection per request with no pool limit.
     * Virtual threads park while waiting for Ollama to respond — no OS threads blocked,
     * no pool to exhaust. Ollama queues calls internally on its side.
     *
     * readTimeout must exceed worst-case LLM response time (~50s for BgGPT on our GPU).
     * Set to 10 minutes to handle large queues of pending Ollama calls.
     */
    private RestClient.Builder ollamaRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofMinutes(10));
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        return OllamaApi.builder()
                .baseUrl(baseUrl)
                .restClientBuilder(ollamaRestClientBuilder())
                .build();
    }

    @Bean
    public OllamaChatModel ollamaChatModel(
            OllamaApi ollamaApi,
            @Value("${spring.ai.ollama.chat.options.model}") String model,
            @Value("${spring.ai.ollama.chat.options.temperature}") Double temperature) {

        var options = OllamaOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();
    }
}
