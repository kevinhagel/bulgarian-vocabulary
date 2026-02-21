package com.vocab.bulgarian.service;

import com.vocab.bulgarian.domain.enums.SentenceStatus;
import com.vocab.bulgarian.repository.LemmaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Registers Micrometer Gauges for current sentence_status distribution.
 * These appear in Prometheus as vocab_sentences_status{status=done|none|queued|generating|failed}
 * and let Grafana chart how the backlog shrinks over time.
 */
@Component
public class SentenceMetricsService {

    public SentenceMetricsService(LemmaRepository lemmaRepository, MeterRegistry meterRegistry) {
        for (SentenceStatus status : SentenceStatus.values()) {
            final SentenceStatus s = status;
            Gauge.builder("vocab.sentences.status", lemmaRepository,
                            r -> r.countBySentenceStatus(s))
                    .tag("status", status.name().toLowerCase())
                    .description("Current number of lemmas with this sentence status")
                    .register(meterRegistry);
        }
    }
}
