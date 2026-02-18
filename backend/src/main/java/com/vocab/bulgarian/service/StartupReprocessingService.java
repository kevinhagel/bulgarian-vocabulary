package com.vocab.bulgarian.service;

import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.domain.enums.ProcessingStatus;
import com.vocab.bulgarian.repository.LemmaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On startup, requeues any lemmas stuck in QUEUED or PROCESSING state
 * (e.g. from a previous crash or connection pool exhaustion).
 */
@Component
public class StartupReprocessingService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupReprocessingService.class);

    private final LemmaRepository lemmaRepository;
    private final BackgroundProcessingService backgroundProcessingService;

    public StartupReprocessingService(LemmaRepository lemmaRepository,
                                      BackgroundProcessingService backgroundProcessingService) {
        this.lemmaRepository = lemmaRepository;
        this.backgroundProcessingService = backgroundProcessingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Lemma> stuck = lemmaRepository.findByProcessingStatusIn(
            List.of(ProcessingStatus.QUEUED, ProcessingStatus.PROCESSING)
        );

        if (stuck.isEmpty()) {
            logger.info("Startup reprocessing: no stuck lemmas found.");
            return;
        }

        logger.info("Startup reprocessing: found {} stuck lemma(s) — requeuing.", stuck.size());
        for (Lemma lemma : stuck) {
            logger.info("  Requeuing lemma ID {} ('{}')", lemma.getId(), lemma.getText());
            backgroundProcessingService.processLemma(lemma.getId());
        }
    }
}
