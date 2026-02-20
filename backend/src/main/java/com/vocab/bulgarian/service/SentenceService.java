package com.vocab.bulgarian.service;

import com.vocab.bulgarian.api.dto.LemmaDetailDTO;
import com.vocab.bulgarian.api.mapper.LemmaMapper;
import com.vocab.bulgarian.domain.ExampleSentence;
import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.domain.enums.SentenceStatus;
import com.vocab.bulgarian.llm.dto.SentenceSet;
import com.vocab.bulgarian.llm.service.SentenceGenerationService;
import com.vocab.bulgarian.repository.LemmaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Orchestrates example sentence generation for vocabulary entries.
 * Uses the same two-transaction pattern as BackgroundProcessingService:
 *  TX1 → mark GENERATING → commit
 *  (no DB connection held)  → call Qwen 2.5 14B  (60-90s)
 *  TX2 → persist sentences → mark DONE → commit
 *
 * NOTE: No class-level @Transactional — backgroundGenerateSentences uses TransactionTemplate
 * directly and must NOT inherit a read-only transaction context. afterCommit callbacks use
 * self-injection (self) to ensure @Async fires through the Spring proxy.
 */
@Service
public class SentenceService {

    private static final Logger logger = LoggerFactory.getLogger(SentenceService.class);

    // Self-reference injected lazily so @Async is applied through the Spring proxy.
    // Calling this.backgroundGenerateSentences() from afterCommit() would bypass the proxy.
    @Lazy
    @Autowired
    private SentenceService self;

    private final LemmaRepository lemmaRepository;
    private final SentenceGenerationService sentenceGenerationService;
    private final LemmaMapper lemmaMapper;
    private final TransactionTemplate txTemplate;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer successTimer;
    private final Timer failureTimer;

    public SentenceService(
            LemmaRepository lemmaRepository,
            SentenceGenerationService sentenceGenerationService,
            LemmaMapper lemmaMapper,
            PlatformTransactionManager transactionManager,
            MeterRegistry meterRegistry) {
        this.lemmaRepository = lemmaRepository;
        this.sentenceGenerationService = sentenceGenerationService;
        this.lemmaMapper = lemmaMapper;
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.successCounter = Counter.builder("vocab.sentences.generated")
                .tag("outcome", "success")
                .description("Words that successfully got example sentences")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("vocab.sentences.generated")
                .tag("outcome", "failure")
                .description("Words that failed sentence generation")
                .register(meterRegistry);
        this.successTimer = Timer.builder("vocab.sentences.total")
                .tag("outcome", "success")
                .description("End-to-end sentence generation duration")
                .register(meterRegistry);
        this.failureTimer = Timer.builder("vocab.sentences.total")
                .tag("outcome", "failure")
                .description("End-to-end sentence generation duration (failed)")
                .register(meterRegistry);
    }

    /**
     * Trigger on-demand sentence generation for a single lemma.
     * Sets status to QUEUED immediately and returns; generation happens in background.
     */
    @Transactional
    public LemmaDetailDTO generateSentences(Long lemmaId) {
        Lemma lemma = lemmaRepository.findByIdWithInflections(lemmaId)
            .orElseThrow(() -> new EntityNotFoundException("Lemma not found with id: " + lemmaId));

        // Clear stale sentences and reset status
        lemma.getExampleSentences().clear();
        lemma.setSentenceStatus(SentenceStatus.QUEUED);
        Lemma saved = lemmaRepository.save(lemma);

        // Trigger background generation after this TX commits.
        // Must call through `self` (the proxy) so @Async is applied.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                self.backgroundGenerateSentences(lemmaId);
            }
        });

        return lemmaMapper.toDetailDTO(saved);
    }

    /**
     * Background sentence generation — runs outside any transaction.
     * TX1: mark GENERATING.  [LLM call — no DB connection held]  TX2: persist sentences, mark DONE.
     */
    @Async("llmTaskExecutor")
    public void backgroundGenerateSentences(Long lemmaId) {
        Instant start = Instant.now();
        Timer.Sample totalSample = Timer.start();
        logger.info("Sentence generation started — lemma ID: {}", lemmaId);

        // TX 1: load lemma info, mark GENERATING
        record LemmaInfo(String text, String translation, String partOfSpeech) {}
        LemmaInfo info = txTemplate.execute(status -> {
            Lemma lemma = lemmaRepository.findById(lemmaId).orElse(null);
            if (lemma == null) {
                logger.error("Lemma ID {} not found for sentence generation", lemmaId);
                return null;
            }
            lemma.setSentenceStatus(SentenceStatus.GENERATING);
            lemmaRepository.save(lemma);
            String pos = lemma.getPartOfSpeech() != null ? lemma.getPartOfSpeech().name() : null;
            return new LemmaInfo(lemma.getText(), lemma.getTranslation(), pos);
        });

        if (info == null) return;

        // ── NO DB CONNECTION HELD DURING LLM CALL ──
        SentenceSet result = null;
        String errorMessage = null;

        try {
            result = sentenceGenerationService.generateSentencesAsync(
                info.text(), info.translation(), info.partOfSpeech()
            ).get();
        } catch (Exception e) {
            logger.error("Sentence generation FAILED for lemma ID {}: {}", lemmaId, e.getMessage(), e);
            errorMessage = e.getMessage();
        }

        // TX 2: persist sentences or record failure
        final SentenceSet finalResult = result;
        final String finalError = errorMessage;

        txTemplate.execute(status -> {
            Lemma lemma = lemmaRepository.findByIdWithInflections(lemmaId).orElse(null);
            if (lemma == null) {
                logger.error("Lemma ID {} disappeared before sentences could be saved", lemmaId);
                return null;
            }

            if (finalError != null || finalResult == null || finalResult.sentences() == null) {
                lemma.setSentenceStatus(SentenceStatus.FAILED);
                lemmaRepository.save(lemma);
                failureCounter.increment();
                totalSample.stop(failureTimer);
                return null;
            }

            // Clear any stale sentences and persist new ones
            lemma.getExampleSentences().clear();
            List<SentenceSet.SentenceEntry> entries = finalResult.sentences();
            for (int i = 0; i < entries.size(); i++) {
                ExampleSentence sentence = new ExampleSentence();
                sentence.setBulgarianText(entries.get(i).bulgarianText());
                sentence.setEnglishTranslation(entries.get(i).englishTranslation());
                sentence.setSortOrder(i);
                lemma.addExampleSentence(sentence);
            }
            lemma.setSentenceStatus(SentenceStatus.DONE);
            lemmaRepository.save(lemma);

            successCounter.increment();
            totalSample.stop(successTimer);
            logger.info("Sentence generation COMPLETED — lemma ID: {}, '{}', {} sentences, {}ms",
                lemmaId, lemma.getText(), entries.size(),
                Duration.between(start, Instant.now()).toMillis());
            return null;
        });
    }

    /**
     * Batch backfill: queue sentence generation for all COMPLETED lemmas that have no sentences.
     * Called from the "Generate All" API endpoint. Runs up to 50 at a time to avoid flooding Ollama.
     *
     * @return number of lemmas queued
     */
    @Transactional
    public int queueBatchGeneration() {
        List<Lemma> lemmas = lemmaRepository.findLemmasNeedingSentences(PageRequest.of(0, 50));
        int count = 0;
        for (Lemma lemma : lemmas) {
            lemma.setSentenceStatus(SentenceStatus.QUEUED);
            lemmaRepository.save(lemma);
            count++;
        }
        logger.info("Batch sentence generation: {} lemmas queued", count);

        // After TX commits, fire off background tasks for each.
        // Must call through `self` (the proxy) so @Async is applied.
        final List<Long> ids = lemmas.stream().map(Lemma::getId).toList();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ids.forEach(id -> self.backgroundGenerateSentences(id));
            }
        });

        return count;
    }
}
