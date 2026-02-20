package com.vocab.bulgarian.llm.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocab.bulgarian.exception.TranslationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Translates Bulgarian text to English via the Google Translate free endpoint.
 * Pure Java HTTP — no Python subprocess required.
 */
@Service
public class TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
    private static final String TRANSLATE_BASE =
            "https://translate.googleapis.com/translate_a/single";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Timer successTimer;
    private final Timer failureTimer;

    public TranslationService(MeterRegistry meterRegistry) {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
        this.successTimer = Timer.builder("vocab.translation.google")
                .tag("outcome", "success")
                .description("Google Translate duration")
                .register(meterRegistry);
        this.failureTimer = Timer.builder("vocab.translation.google")
                .tag("outcome", "failure")
                .description("Google Translate duration (failed)")
                .register(meterRegistry);
    }

    @Cacheable(value = "translations", key = "#bulgarianText.trim().toLowerCase()")
    public String translate(String bulgarianText) {
        if (bulgarianText == null || bulgarianText.isBlank()) {
            throw new TranslationException("Bulgarian text cannot be empty");
        }

        Timer.Sample sample = Timer.start();
        try {
            // UriComponentsBuilder handles Cyrillic encoding correctly — no manual URLEncoder needed
            URI uri = UriComponentsBuilder.fromUriString(TRANSLATE_BASE)
                    .queryParam("client", "gtx")
                    .queryParam("sl", "bg")
                    .queryParam("tl", "en")
                    .queryParam("dt", "t")
                    .queryParam("q", bulgarianText.trim())
                    .build()
                    .encode()
                    .toUri();

            // Response format: [[["translation","original",...]],...,"bg"]
            String response = restClient.get()
                    .uri(uri)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String translation = root.get(0).get(0).get(0).asText();

            if (translation == null || translation.isBlank()) {
                throw new TranslationException("Translation returned empty result");
            }

            logger.debug("Translation: {} → {}", bulgarianText, translation);
            sample.stop(successTimer);
            return translation;

        } catch (Exception e) {
            sample.stop(failureTimer);
            logger.error("Translation failed for text: {}", bulgarianText, e);
            throw new TranslationException("Translation failed: " + e.getMessage(), e);
        }
    }

    public String translateWithFallback(String bulgarianText) {
        try {
            return translate(bulgarianText);
        } catch (Exception e) {
            logger.warn("Translation failed for '{}', returning null: {}", bulgarianText, e.getMessage());
            return null;
        }
    }
}
