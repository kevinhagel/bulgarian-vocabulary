package com.vocab.bulgarian.service;

import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.domain.enums.SentenceStatus;
import com.vocab.bulgarian.repository.LemmaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * On startup, resumes sentence generation for any lemmas stuck in QUEUED or
 * GENERATING state (e.g. from a previous crash or backend restart mid-generation).
 *
 * Runs after StartupReprocessingService (order 2 vs default 0) to avoid
 * competing with inflection processing for Ollama resources at startup.
 */
@Component
@Order(2)
public class StartupSentenceService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupSentenceService.class);

    private final LemmaRepository lemmaRepository;
    private final SentenceService sentenceService;

    public StartupSentenceService(LemmaRepository lemmaRepository, SentenceService sentenceService) {
        this.lemmaRepository = lemmaRepository;
        this.sentenceService = sentenceService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Lemma> pending = lemmaRepository.findBySentenceStatusIn(
            List.of(SentenceStatus.QUEUED, SentenceStatus.GENERATING)
        );

        if (pending.isEmpty()) {
            logger.info("Startup sentence service: no pending sentence generation.");
            return;
        }

        // Reset any stuck GENERATING back to QUEUED so they retry cleanly
        int reset = 0;
        for (Lemma lemma : pending) {
            if (lemma.getSentenceStatus() == SentenceStatus.GENERATING) {
                lemma.setSentenceStatus(SentenceStatus.QUEUED);
                lemmaRepository.save(lemma);
                reset++;
            }
        }
        if (reset > 0) {
            logger.info("Startup sentence service: reset {} stuck GENERATING lemma(s) to QUEUED.", reset);
        }

        logger.info("Startup sentence service: firing sentence generation for {} lemma(s).", pending.size());
        for (Lemma lemma : pending) {
            logger.info("  Queuing sentences for lemma ID {} ('{}')", lemma.getId(), lemma.getText());
            sentenceService.backgroundGenerateSentences(lemma.getId());
        }
    }
}
