package com.vocab.bulgarian.llm.service;

import com.vocab.bulgarian.llm.dto.InflectionSet;
import com.vocab.bulgarian.llm.dto.LemmaDetectionResponse;
import com.vocab.bulgarian.llm.dto.LemmaMetadata;
import com.vocab.bulgarian.llm.dto.LlmProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrates the complete LLM pipeline for new vocabulary entry processing.
 * Composes async calls: lemma detection -> parallel (inflections + metadata).
 * Handles partial failures gracefully and returns combined result for user review.
 */
@Service
public class LlmOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(LlmOrchestrationService.class);

    private final LemmaDetectionService lemmaDetectionService;
    private final InflectionGenerationService inflectionGenerationService;
    private final MetadataGenerationService metadataGenerationService;

    public LlmOrchestrationService(
        LemmaDetectionService lemmaDetectionService,
        InflectionGenerationService inflectionGenerationService,
        MetadataGenerationService metadataGenerationService
    ) {
        this.lemmaDetectionService = lemmaDetectionService;
        this.inflectionGenerationService = inflectionGenerationService;
        this.metadataGenerationService = metadataGenerationService;
    }

    /**
     * Processes a new word through the complete LLM pipeline.
     *
     * Pipeline stages:
     * 1. Detect lemma from word form
     * 2. If successful, fan out to parallel:
     *    - Generate inflections for lemma
     *    - Generate metadata for lemma
     * 3. Combine results with partial failure handling
     *
     * @param wordForm the Bulgarian word form to process
     * @return CompletableFuture containing the combined processing result
     */
    public CompletableFuture<LlmProcessingResult> processNewWord(String wordForm) {
        log.info("Processing new word: {}", wordForm);

        // Step 1: Detect lemma
        return lemmaDetectionService.detectLemmaAsync(wordForm)
            .thenCompose(lemmaDetection -> {
                // Check if lemma detection failed
                if (lemmaDetection.detectionFailed()) {
                    log.warn("Lemma detection failed for: {}", wordForm);
                    List<String> warnings = List.of("Lemma detection failed");
                    return CompletableFuture.completedFuture(
                        new LlmProcessingResult(wordForm, lemmaDetection, null, null, false, warnings)
                    );
                }

                log.info("Lemma detected: {} for word form: {}", lemmaDetection.lemma(), wordForm);

                // Step 2: Fan out to parallel inflection and metadata generation
                CompletableFuture<InflectionSet> inflectionsFuture =
                    inflectionGenerationService.generateInflectionsAsync(
                        lemmaDetection.lemma(),
                        lemmaDetection.partOfSpeech()
                    );

                CompletableFuture<LemmaMetadata> metadataFuture =
                    metadataGenerationService.generateMetadataAsync(lemmaDetection.lemma());

                // Step 3: Combine results
                return inflectionsFuture.thenCombine(metadataFuture, (inflections, metadata) -> {
                    List<String> warnings = new ArrayList<>();
                    boolean fullySuccessful = true;

                    // Check for partial failures
                    if (inflections == null) {
                        warnings.add("Inflection generation failed");
                        fullySuccessful = false;
                        log.warn("Inflection generation failed for: {}", lemmaDetection.lemma());
                    }

                    if (metadata == null) {
                        warnings.add("Metadata generation failed");
                        fullySuccessful = false;
                        log.warn("Metadata generation failed for: {}", lemmaDetection.lemma());
                    }

                    log.info("Processing complete for {}: fullySuccessful={}", wordForm, fullySuccessful);

                    return new LlmProcessingResult(
                        wordForm,
                        lemmaDetection,
                        inflections,
                        metadata,
                        fullySuccessful,
                        warnings
                    );
                });
            })
            .exceptionally(ex -> {
                log.error("Unexpected error processing word {}: {}", wordForm, ex.getMessage(), ex);
                List<String> warnings = List.of("Processing failed: " + ex.getMessage());
                return new LlmProcessingResult(
                    wordForm,
                    LemmaDetectionResponse.failed(wordForm),
                    null,
                    null,
                    false,
                    warnings
                );
            });
    }
}
