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
        logger.info("Starting background processing for lemma ID: {}", lemmaId);

        Lemma lemma = lemmaRepository.findById(lemmaId).orElse(null);
        if (lemma == null) {
            logger.error("Lemma ID {} not found, skipping processing", lemmaId);
            return;
        }

        try {
            // Update status to PROCESSING
            lemma.setProcessingStatus(ProcessingStatus.PROCESSING);
            lemmaRepository.save(lemma);

            // Step 1: Process word form through LLM pipeline (lemma detection + inflections + metadata)
            String userInput = lemma.getText();
            logger.debug("Processing word form through LLM pipeline: {}", userInput);

            LlmProcessingResult result = llmOrchestrationService.processNewWord(userInput)
                .get(); // Block and wait for async result

            // Step 2: Update lemma text to detected canonical form
            if (result.lemmaDetection() != null && result.lemmaDetection().lemma() != null) {
                String detectedLemma = result.lemmaDetection().lemma();
                lemma.setText(detectedLemma);
                logger.debug("Detected canonical lemma: {}", detectedLemma);
            } else {
                throw new RuntimeException("Failed to detect lemma from input: " + userInput);
            }

            // Step 3: Auto-translate if translation is missing
            if (lemma.getTranslation() == null || lemma.getTranslation().isBlank()) {
                logger.debug("Translating lemma to English");
                String translation = translationService.translateWithFallback(lemma.getText());
                if (translation != null) {
                    lemma.setTranslation(translation);
                    logger.debug("Translation: {}", translation);
                } else {
                    logger.warn("Translation failed, leaving translation null");
                }
            }

            // Step 4: Apply inflections
            if (result.inflections() != null && !result.inflections().inflections().isEmpty()) {
                // Clear existing inflections
                lemma.getInflections().clear();

                // Add new inflections
                for (var inflectionEntry : result.inflections().inflections()) {
                    Inflection inflection = new Inflection();
                    inflection.setForm(inflectionEntry.text());
                    inflection.setGrammaticalInfo(inflectionEntry.grammaticalTags());
                    inflection.setDifficultyLevel(inflectionEntry.difficultyLevel());
                    lemma.addInflection(inflection);
                }
                logger.debug("Added {} inflections", result.inflections().inflections().size());
            } else {
                logger.warn("No inflections generated for lemma: {}", lemma.getText());
            }

            // Step 5: Apply metadata (part of speech, category, difficulty)
            if (result.metadata() != null) {
                LemmaMetadata metadata = result.metadata();

                if (metadata.partOfSpeech() != null && !metadata.partOfSpeech().isBlank()) {
                    try {
                        PartOfSpeech pos = PartOfSpeech.valueOf(metadata.partOfSpeech().toUpperCase());
                        lemma.setPartOfSpeech(pos);
                        logger.debug("Part of speech: {}", pos);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid part of speech '{}', leaving null", metadata.partOfSpeech());
                    }
                }

                if (metadata.category() != null && !metadata.category().isBlank()) {
                    lemma.setCategory(metadata.category());
                    logger.debug("Category: {}", metadata.category());
                }

                if (metadata.difficultyLevel() != null && !metadata.difficultyLevel().isBlank()) {
                    try {
                        DifficultyLevel difficulty = DifficultyLevel.valueOf(metadata.difficultyLevel().toUpperCase());
                        lemma.setDifficultyLevel(difficulty);
                        logger.debug("Difficulty level: {}", difficulty);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid difficulty level '{}', leaving null", metadata.difficultyLevel());
                    }
                }
            }

            // Step 6: Mark as completed
            lemma.setProcessingStatus(ProcessingStatus.COMPLETED);
            lemma.setProcessingError(null);
            lemmaRepository.save(lemma);

            logger.info("Background processing completed successfully for lemma ID: {}", lemmaId);

        } catch (Exception e) {
            logger.error("Background processing failed for lemma ID: {}", lemmaId, e);

            // Mark as failed with error message
            lemma.setProcessingStatus(ProcessingStatus.FAILED);
            lemma.setProcessingError(e.getMessage());
            lemmaRepository.save(lemma);
        }
    }
}
