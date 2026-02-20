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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Service for background processing of vocabulary entries.
 * Orchestrates LLM pipeline: lemma detection → translation → inflections → metadata.
 *
 * DB connections are held only during short fetch/save operations.
 * The slow Ollama LLM call runs outside any transaction so connections are
 * returned to the pool while inference is in progress.
 */
@Service
public class BackgroundProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(BackgroundProcessingService.class);

    private final LlmOrchestrationService llmOrchestrationService;
    private final TranslationService translationService;
    private final LemmaRepository lemmaRepository;
    private final TransactionTemplate txTemplate;
    private final Timer totalSuccessTimer;
    private final Timer totalFailureTimer;
    private final Counter successCounter;
    private final Counter failureCounter;

    public BackgroundProcessingService(
            LlmOrchestrationService llmOrchestrationService,
            TranslationService translationService,
            LemmaRepository lemmaRepository,
            PlatformTransactionManager transactionManager,
            MeterRegistry meterRegistry) {
        this.llmOrchestrationService = llmOrchestrationService;
        this.translationService = translationService;
        this.lemmaRepository = lemmaRepository;
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.totalSuccessTimer = Timer.builder("vocab.processing.total")
                .tag("outcome", "success")
                .description("End-to-end word processing duration")
                .register(meterRegistry);
        this.totalFailureTimer = Timer.builder("vocab.processing.total")
                .tag("outcome", "failure")
                .description("End-to-end word processing duration (failed)")
                .register(meterRegistry);
        this.successCounter = Counter.builder("vocab.processing.words")
                .tag("outcome", "success")
                .description("Words successfully processed")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("vocab.processing.words")
                .tag("outcome", "failure")
                .description("Words that failed processing")
                .register(meterRegistry);
    }

    /**
     * Process a vocabulary entry in the background.
     * Runs asynchronously to avoid blocking the user.
     *
     * @param lemmaId the ID of the lemma to process
     */
    @Async("llmTaskExecutor")
    public void processLemma(Long lemmaId) {
        Instant totalStart = Instant.now();
        Timer.Sample totalSample = Timer.start();
        logger.info("Background processing started — lemma ID: {}", lemmaId);

        // Short TX 1: fetch lemma, mark PROCESSING, release connection immediately
        record WordInput(String text, String translationHint) {}
        WordInput wordInput = txTemplate.execute(status -> {
            Lemma lemma = lemmaRepository.findById(lemmaId).orElse(null);
            if (lemma == null) {
                logger.error("Lemma ID {} not found, skipping processing", lemmaId);
                return null;
            }
            lemma.setProcessingStatus(ProcessingStatus.PROCESSING);
            lemmaRepository.save(lemma);
            // Combine translation + notes into a single hint string for LLM disambiguation
            String hint = Stream.of(lemma.getTranslation(), lemma.getNotes())
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining("; "));
            return new WordInput(lemma.getText(), hint.isBlank() ? null : hint);
        });

        if (wordInput == null) return;
        String userInput = wordInput.text();
        String translationHint = wordInput.translationHint();

        // ── NO DB CONNECTION HELD BELOW THIS LINE DURING LLM CALLS ──

        LlmProcessingResult result = null;
        String translation = null;
        String errorMessage = null;

        try {
            // Step 1: LLM pipeline (lemma detection + inflections + metadata)
            Instant step1Start = Instant.now();
            logger.info("[1/5] LLM pipeline starting for input: '{}'", userInput);
            result = llmOrchestrationService.processNewWord(userInput, translationHint).get();
            logger.info("[1/5] LLM pipeline completed in {}ms", Duration.between(step1Start, Instant.now()).toMillis());

            if (result.lemmaDetection() == null || result.lemmaDetection().lemma() == null) {
                throw new RuntimeException("Failed to detect lemma from input: " + userInput);
            }

            String detectedLemma = result.lemmaDetection().lemma();
            logger.info("[2/5] Canonical lemma: '{}' → '{}'", userInput, detectedLemma);

            // Step 3: Translation (also outside TX — may involve a network call)
            Instant step3Start = Instant.now();
            translation = translationService.translateWithFallback(detectedLemma, translationHint);
            logger.info("[3/5] Translation: '{}' ({}ms)", translation, Duration.between(step3Start, Instant.now()).toMillis());

        } catch (Exception e) {
            long elapsed = Duration.between(totalStart, Instant.now()).toMillis();
            logger.error("Background processing FAILED — lemma ID: {}, after {}ms: {}", lemmaId, elapsed, e.getMessage(), e);
            errorMessage = e.getMessage();
        }

        // Short TX 2: save all results (or failure), release connection immediately
        final LlmProcessingResult finalResult = result;
        final String finalTranslation = translation;
        final String finalError = errorMessage;

        txTemplate.execute(status -> {
            Lemma lemma = lemmaRepository.findById(lemmaId).orElse(null);
            if (lemma == null) {
                logger.error("Lemma ID {} disappeared before results could be saved", lemmaId);
                return null;
            }

            if (finalError != null) {
                lemma.setProcessingStatus(ProcessingStatus.FAILED);
                lemma.setProcessingError(finalError);
                lemmaRepository.save(lemma);
                totalSample.stop(totalFailureTimer);
                failureCounter.increment();
                return null;
            }

            // Step 2: Canonical lemma text
            lemma.setText(finalResult.lemmaDetection().lemma());

            // Step 3: Translation
            if (finalTranslation != null && (lemma.getTranslation() == null || lemma.getTranslation().isBlank())) {
                lemma.setTranslation(finalTranslation);
            }

            // Step 4: Inflections
            Instant step4Start = Instant.now();
            if (finalResult.inflections() != null && !finalResult.inflections().inflections().isEmpty()) {
                lemma.getInflections().clear();
                for (var entry : finalResult.inflections().inflections()) {
                    Inflection inflection = new Inflection();
                    inflection.setForm(entry.text());
                    inflection.setGrammaticalInfo(entry.grammaticalTags());
                    inflection.setDifficultyLevel(entry.difficultyLevel());
                    lemma.addInflection(inflection);
                }
                logger.info("[4/5] {} inflections applied ({}ms)",
                    finalResult.inflections().inflections().size(), Duration.between(step4Start, Instant.now()).toMillis());
            } else {
                logger.warn("[4/5] No inflections generated for lemma: '{}'", lemma.getText());
            }

            // Step 5: Metadata
            Instant step5Start = Instant.now();
            if (finalResult.metadata() != null) {
                LemmaMetadata metadata = finalResult.metadata();
                if (metadata.partOfSpeech() != null && !metadata.partOfSpeech().isBlank()) {
                    try {
                        lemma.setPartOfSpeech(PartOfSpeech.valueOf(metadata.partOfSpeech().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[5/5] Invalid part of speech '{}', leaving null", metadata.partOfSpeech());
                    }
                }
                if (metadata.category() != null && !metadata.category().isBlank()) {
                    lemma.setCategory(metadata.category());
                }
                if (metadata.difficultyLevel() != null && !metadata.difficultyLevel().isBlank()) {
                    try {
                        lemma.setDifficultyLevel(DifficultyLevel.valueOf(metadata.difficultyLevel().toUpperCase()));
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

            totalSample.stop(totalSuccessTimer);
            successCounter.increment();
            logger.info("Background processing COMPLETED — lemma ID: {}, '{}', total: {}ms",
                lemmaId, lemma.getText(), Duration.between(totalStart, Instant.now()).toMillis());

            return null;
        });
    }
}
