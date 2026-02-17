package com.vocab.bulgarian.service;

import com.vocab.bulgarian.domain.Inflection;
import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.domain.enums.DifficultyLevel;
import com.vocab.bulgarian.domain.enums.PartOfSpeech;
import com.vocab.bulgarian.domain.enums.ProcessingStatus;
import com.vocab.bulgarian.llm.dto.LemmaMetadata;
import com.vocab.bulgarian.llm.dto.LlmProcessingResult;
import com.vocab.bulgarian.llm.service.LlmOrchestrationService;
import com.vocab.bulgarian.llm.translation.TranslationService;
import com.vocab.bulgarian.repository.LemmaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;

/**
 * Service for background processing of vocabulary entries.
 * Orchestrates LLM pipeline: lemma detection → translation → inflections → metadata.
 */
@Service
public class BackgroundProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(BackgroundProcessingService.class);

    private final LlmOrchestrationService llmOrchestrationService;
    private final TranslationService translationService;
    private final LemmaRepository lemmaRepository;

    public BackgroundProcessingService(
            LlmOrchestrationService llmOrchestrationService,
            TranslationService translationService,
            LemmaRepository lemmaRepository) {
        this.llmOrchestrationService = llmOrchestrationService;
        this.translationService = translationService;
        this.lemmaRepository = lemmaRepository;
    }

    /**
     * Process a vocabulary entry in the background.
     * Runs asynchronously to avoid blocking the user.
     *
     * @param lemmaId the ID of the lemma to process
     */
    @Async("llmTaskExecutor")
    @Transactional
    public void processLemma(Long lemmaId) {
        Instant totalStart = Instant.now();
        logger.info("Background processing started — lemma ID: {}", lemmaId);

        Lemma lemma = lemmaRepository.findById(lemmaId).orElse(null);
        if (lemma == null) {
            logger.error("Lemma ID {} not found, skipping processing", lemmaId);
            return;
        }

        try {
            // Update status to PROCESSING
            lemma.setProcessingStatus(ProcessingStatus.PROCESSING);
            lemmaRepository.save(lemma);

            // Step 1: LLM pipeline (lemma detection + inflections + metadata)
            String userInput = lemma.getText();
            Instant step1Start = Instant.now();
            logger.info("[1/5] LLM pipeline starting for input: '{}'", userInput);

            LlmProcessingResult result = llmOrchestrationService.processNewWord(userInput)
                .get();

            logger.info("[1/5] LLM pipeline completed in {}ms", Duration.between(step1Start, Instant.now()).toMillis());

            // Step 2: Canonical lemma
            Instant step2Start = Instant.now();
            if (result.lemmaDetection() != null && result.lemmaDetection().lemma() != null) {
                String detectedLemma = result.lemmaDetection().lemma();
                logger.info("[2/5] Canonical lemma detected: '{}' → '{}' ({}ms)",
                    userInput, detectedLemma, Duration.between(step2Start, Instant.now()).toMillis());
                lemma.setText(detectedLemma);
            } else {
                throw new RuntimeException("Failed to detect lemma from input: " + userInput);
            }

            // Step 3: Translation
            Instant step3Start = Instant.now();
            if (lemma.getTranslation() == null || lemma.getTranslation().isBlank()) {
                String translation = translationService.translateWithFallback(lemma.getText());
                if (translation != null) {
                    lemma.setTranslation(translation);
                    logger.info("[3/5] Translation: '{}' ({}ms)",
                        translation, Duration.between(step3Start, Instant.now()).toMillis());
                } else {
                    logger.warn("[3/5] Translation failed for '{}' ({}ms)",
                        lemma.getText(), Duration.between(step3Start, Instant.now()).toMillis());
                }
            } else {
                logger.info("[3/5] Translation already present, skipping");
            }

            // Step 4: Inflections
            Instant step4Start = Instant.now();
            if (result.inflections() != null && !result.inflections().inflections().isEmpty()) {
                lemma.getInflections().clear();
                for (var inflectionEntry : result.inflections().inflections()) {
                    Inflection inflection = new Inflection();
                    inflection.setForm(inflectionEntry.text());
                    inflection.setGrammaticalInfo(inflectionEntry.grammaticalTags());
                    inflection.setDifficultyLevel(inflectionEntry.difficultyLevel());
                    lemma.addInflection(inflection);
                }
                logger.info("[4/5] {} inflections applied ({}ms)",
                    result.inflections().inflections().size(), Duration.between(step4Start, Instant.now()).toMillis());
            } else {
                logger.warn("[4/5] No inflections generated for lemma: '{}'", lemma.getText());
            }

            // Step 5: Metadata
            Instant step5Start = Instant.now();
            if (result.metadata() != null) {
                LemmaMetadata metadata = result.metadata();

                if (metadata.partOfSpeech() != null && !metadata.partOfSpeech().isBlank()) {
                    try {
                        PartOfSpeech pos = PartOfSpeech.valueOf(metadata.partOfSpeech().toUpperCase());
                        lemma.setPartOfSpeech(pos);
                    } catch (IllegalArgumentException e) {
                        logger.warn("[5/5] Invalid part of speech '{}', leaving null", metadata.partOfSpeech());
                    }
                }

                if (metadata.category() != null && !metadata.category().isBlank()) {
                    lemma.setCategory(metadata.category());
                }

                if (metadata.difficultyLevel() != null && !metadata.difficultyLevel().isBlank()) {
                    try {
                        DifficultyLevel difficulty = DifficultyLevel.valueOf(metadata.difficultyLevel().toUpperCase());
                        lemma.setDifficultyLevel(difficulty);
                    } catch (IllegalArgumentException e) {
                        logger.warn("[5/5] Invalid difficulty level '{}', leaving null", metadata.difficultyLevel());
                    }
                }
                logger.info("[5/5] Metadata applied — POS: {}, category: {}, difficulty: {} ({}ms)",
                    lemma.getPartOfSpeech(), lemma.getCategory(), lemma.getDifficultyLevel(),
                    Duration.between(step5Start, Instant.now()).toMillis());
            }

            lemma.setProcessingStatus(ProcessingStatus.COMPLETED);
            lemma.setProcessingError(null);
            lemmaRepository.save(lemma);

            logger.info("Background processing COMPLETED — lemma ID: {}, '{}', total: {}ms",
                lemmaId, lemma.getText(), Duration.between(totalStart, Instant.now()).toMillis());

        } catch (Exception e) {
            long elapsed = Duration.between(totalStart, Instant.now()).toMillis();
            logger.error("Background processing FAILED — lemma ID: {}, after {}ms: {}",
                lemmaId, elapsed, e.getMessage(), e);

            lemma.setProcessingStatus(ProcessingStatus.FAILED);
            lemma.setProcessingError(e.getMessage());
            lemmaRepository.save(lemma);
        }
    }
}
