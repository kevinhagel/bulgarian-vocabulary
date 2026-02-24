package com.vocab.bulgarian.study.service;

import com.vocab.bulgarian.domain.enums.Source;
import com.vocab.bulgarian.repository.LemmaRepository;
import com.vocab.bulgarian.study.domain.enums.SessionStatus;
import com.vocab.bulgarian.study.dto.LemmaStatsDTO;
import com.vocab.bulgarian.study.dto.ProgressDashboardDTO;
import com.vocab.bulgarian.study.repository.SrsStateRepository;
import com.vocab.bulgarian.study.repository.StudyReviewRepository;
import com.vocab.bulgarian.study.repository.StudySessionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class ProgressService {

    private final LemmaRepository lemmaRepository;
    private final SrsStateRepository srsStateRepository;
    private final StudySessionRepository sessionRepository;
    private final StudyReviewRepository reviewRepository;

    public ProgressService(
        LemmaRepository lemmaRepository,
        SrsStateRepository srsStateRepository,
        StudySessionRepository sessionRepository,
        StudyReviewRepository reviewRepository
    ) {
        this.lemmaRepository = lemmaRepository;
        this.srsStateRepository = srsStateRepository;
        this.sessionRepository = sessionRepository;
        this.reviewRepository = reviewRepository;
    }

    public ProgressDashboardDTO getDashboard() {
        long totalUserVocab = lemmaRepository.countBySource(Source.USER_ENTERED);
        long totalVocabStudied = srsStateRepository.count();
        long completedSessions = sessionRepository.countByStatus(SessionStatus.COMPLETED);
        long abandonedSessions = sessionRepository.countByStatus(SessionStatus.ABANDONED);
        long totalSessions = completedSessions + abandonedSessions;
        long totalCardsReviewed = sessionRepository.sumTotalCardsReviewed();
        long totalCorrect = sessionRepository.sumTotalCorrect();
        int retentionRate = totalCardsReviewed > 0
            ? (int) Math.round((double) totalCorrect / totalCardsReviewed * 100)
            : 0;
        long cardsDueToday = srsStateRepository.countDueCards(LocalDate.now());
        long newCards = srsStateRepository.countLemmaIdsWithoutSrsState();
        return new ProgressDashboardDTO(totalUserVocab, totalVocabStudied, totalSessions,
            totalCardsReviewed, totalCorrect, retentionRate, cardsDueToday, newCards);
    }

    public LemmaStatsDTO getLemmaStats(Long lemmaId) {
        if (!lemmaRepository.existsById(lemmaId)) {
            throw new EntityNotFoundException("Lemma not found: " + lemmaId);
        }
        long reviewCount = reviewRepository.countByLemmaId(lemmaId);
        long correctCount = reviewRepository.countCorrectByLemmaId(lemmaId);
        int correctRate = reviewCount > 0
            ? (int) Math.round((double) correctCount / reviewCount * 100)
            : 0;
        return srsStateRepository.findByLemmaId(lemmaId)
            .map(srs -> new LemmaStatsDTO(lemmaId, reviewCount, correctCount, correctRate,
                srs.getLastReviewedAt(), srs.getNextReviewDate(),
                srs.getIntervalDays(), srs.getEaseFactor().doubleValue()))
            .orElse(new LemmaStatsDTO(lemmaId, reviewCount, correctCount, correctRate,
                null, null, 0, 0.0));
    }
}
