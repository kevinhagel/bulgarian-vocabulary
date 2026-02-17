package com.vocab.bulgarian.llm.translation;

import com.vocab.bulgarian.exception.TranslationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Service for translating Bulgarian text to English using Google Translate.
 * Uses deep-translator Python library via subprocess for translation.
 */
@Service
public class TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
    private static final int TIMEOUT_SECONDS = 10;

    /**
     * Translate Bulgarian text to English.
     * Results are cached to avoid redundant API calls.
     *
     * @param bulgarianText the Bulgarian text to translate
     * @return English translation
     * @throws TranslationException if translation fails
     */
    @Cacheable(value = "translations", key = "#bulgarianText.trim().toLowerCase()")
    public String translate(String bulgarianText) {
        if (bulgarianText == null || bulgarianText.isBlank()) {
            throw new TranslationException("Bulgarian text cannot be empty");
        }

        try {
            logger.debug("Translating Bulgarian text: {}", bulgarianText);

            // Use deep-translator Python library via subprocess
            ProcessBuilder pb = new ProcessBuilder(
                "python3",
                "-c",
                "from deep_translator import GoogleTranslator; " +
                "import sys; " +
                "text = sys.stdin.read(); " +
                "print(GoogleTranslator(source='bg', target='en').translate(text), end='')"
            );

            Process process = pb.start();

            // Write Bulgarian text to stdin
            try (var writer = process.outputWriter(StandardCharsets.UTF_8)) {
                writer.write(bulgarianText);
                writer.flush();
            }

            // Read translation from stdout
            StringBuilder output = new StringBuilder();
            try (var reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            // Read any errors from stderr
            StringBuilder errorOutput = new StringBuilder();
            try (var errorReader = new BufferedReader(new InputStreamReader(
                    process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TranslationException("Translation timed out after " + TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("Translation failed with exit code {}: {}", exitCode, errorOutput);
                throw new TranslationException("Translation process failed: " + errorOutput);
            }

            String translation = output.toString().trim();
            if (translation.isEmpty()) {
                throw new TranslationException("Translation returned empty result");
            }

            logger.debug("Translation successful: {} -> {}", bulgarianText, translation);
            return translation;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException("Translation interrupted", e);
        } catch (Exception e) {
            logger.error("Translation failed for text: {}", bulgarianText, e);
            throw new TranslationException("Translation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Translate Bulgarian text to English with fallback to null on failure.
     * Used in background processing where graceful degradation is preferred.
     *
     * @param bulgarianText the Bulgarian text to translate
     * @return English translation or null if translation fails
     */
    public String translateWithFallback(String bulgarianText) {
        try {
            return translate(bulgarianText);
        } catch (Exception e) {
            logger.warn("Translation failed for '{}', returning null: {}",
                        bulgarianText, e.getMessage());
            return null;
        }
    }
}
