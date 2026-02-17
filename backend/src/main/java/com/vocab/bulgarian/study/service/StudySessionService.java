package com.vocab.bulgarian.study.service;

import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.repository.LemmaRepository;
import com.vocab.bulgarian.study.domain.*;
import com.vocab.bulgarian.study.domain.enums.ReviewRating;
import com.vocab.bulgarian.study.domain.enums.SessionStatus;
import com.vocab.bulgarian.study.dto.*;
import com.vocab.bulgarian.study.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Transactional
public class StudySessionService {

    private static final Logger log = LoggerFactory.getLogger(StudySessionService.class);
    private static final int MAX_NEW_CARDS_PER_SESSION = 10;

    private final StudySessionRepository sessionRepo;
    private final SessionCardRepository cardRepo;
    private final SrsStateRepository srsRepo;
    private final StudyReviewRepository reviewRepo;
    private final LemmaRepository lemmaRepo;
    private final SrsAlgorithmService algorithm;

    public StudySessionService(
        StudySessionRepository sessionRepo,
        SessionCardRepository cardRepo,
        SrsStateRepository srsRepo,
        StudyReviewRepository reviewRepo,
        LemmaRepository lemmaRepo,
        SrsAlgorithmService algorithm
    ) {
        this.sessionRepo = sessionRepo;
        this.cardRepo = cardRepo;
        this.srsRepo = srsRepo;
        this.reviewRepo = reviewRepo;
        this.lemmaRepo = lemmaRepo;
        this.algorithm = algorithm;
    }

    public StartSessionResponseDTO startSession(int maxCards) {
        // Collect due cards
        List<Long> dueLemmaIds = srsRepo.findDueCards(LocalDate.now())
            .stream().map(s -> s.getLemma().getId()).toList();

        // Collect new cards (no SrsState yet), capped
        List<Long> newLemmaIds = srsRepo.findLemmaIdsWithoutSrsState()
            .stream().limit(MAX_NEW_CARDS_PER_SESSION).toList();

        // Merge, deduplicate, shuffle, cap
        Set<Long> allIds = new LinkedHashSet<>(dueLemmaIds);
        allIds.addAll(newLemmaIds);
        List<Long> shuffled = new ArrayList<>(allIds);
        Collections.shuffle(shuffled);
        List<Long> sessionIds = shuffled.stream().limit(maxCards).toList();

        if (sessionIds.isEmpty()) {
            throw new IllegalStateException(
                "No cards available for study. Add vocabulary or wait for scheduled review dates.");
        }

        // Ensure SrsState exists for all new cards
        for (Long lemmaId : newLemmaIds) {
            if (srsRepo.findByLemmaId(lemmaId).isEmpty()) {
                Lemma lemma = lemmaRepo.getReferenceById(lemmaId);
                SrsState state = new SrsState();
                state.setLemma(lemma);
                srsRepo.save(state);
            }
        }

        // Build session
        StudySession session = new StudySession();
        session.setCardCount(sessionIds.size());
        session = sessionRepo.save(session);

        for (int i = 0; i < sessionIds.size(); i++) {
            Lemma lemma = lemmaRepo.getReferenceById(sessionIds.get(i));
            SessionCard card = new SessionCard();
            card.setSession(session);
            card.setLemma(lemma);
            card.setPosition(i);
            cardRepo.save(card);
        }

        log.info("Study session {} started: {} due, {} new, {} total cards",
            session.getId(), dueLemmaIds.size(), newLemmaIds.size(), sessionIds.size());

        StudyCardDTO firstCard = getNextCard(session.getId());
        return new StartSessionResponseDTO(session.getId(), session.getCardCount(), firstCard);
    }

    @Transactional(readOnly = true)
    public StudyCardDTO getNextCard(Long sessionId) {
        return findFirstUnreviewed(sessionId)
            .map(sc -> toCardDTO(sc.getLemma(), sessionId))
            .orElse(null);
    }

    public StudyCardDTO rateCard(Long sessionId, Long lemmaId, ReviewRating rating) {
        StudySession session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Session " + sessionId + " is not active");
        }

        // Mark card reviewed
        SessionCardId cardId = new SessionCardId();
        cardId.setSessionId(sessionId);
        cardId.setLemmaId(lemmaId);
        SessionCard card = cardRepo.findById(cardId)
            .orElseThrow(() -> new EntityNotFoundException("Card not found in session"));
        card.setReviewed(true);
        cardRepo.save(card);

        // Update SRS state
        SrsState state = srsRepo.findByLemmaId(lemmaId)
            .orElseThrow(() -> new EntityNotFoundException("SRS state not found for lemma: " + lemmaId));
        algorithm.applyRating(state, rating);
        srsRepo.save(state);

        // Record review event
        Lemma lemma = lemmaRepo.getReferenceById(lemmaId);
        StudyReview review = new StudyReview();
        review.setSession(session);
        review.setLemma(lemma);
        review.setRating(rating);
        reviewRepo.save(review);

        // Update session counters
        session.setCardsReviewed(session.getCardsReviewed() + 1);
        if (rating == ReviewRating.CORRECT) {
            session.setCorrectCount(session.getCorrectCount() + 1);
        }
        sessionRepo.save(session);

        log.info("Session {} card {} rated {} — interval now {}d",
            sessionId, lemmaId, rating, state.getIntervalDays());

        // Auto-complete if all reviewed
        if (findFirstUnreviewed(sessionId).isEmpty()) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndedAt(ZonedDateTime.now());
            sessionRepo.save(session);
            log.info("Session {} auto-completed", sessionId);
        }

        return getNextCard(sessionId);
    }

    public SessionSummaryDTO endSession(Long sessionId) {
        StudySession session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));

        if (session.getStatus() == SessionStatus.ACTIVE) {
            session.setStatus(SessionStatus.ABANDONED);
            session.setEndedAt(ZonedDateTime.now());
            sessionRepo.save(session);
        }

        int retentionRate = session.getCardsReviewed() > 0
            ? (int) Math.round((double) session.getCorrectCount() / session.getCardsReviewed() * 100)
            : 0;

        return new SessionSummaryDTO(
            session.getId(),
            session.getStatus().name(),
            session.getCardCount(),
            session.getCardsReviewed(),
            session.getCorrectCount(),
            retentionRate
        );
    }

    @Transactional(readOnly = true)
    public DueCountDTO getDueCount() {
        long dueToday = srsRepo.countDueCards(LocalDate.now());
        long newCards = srsRepo.findLemmaIdsWithoutSrsState().size();
        return new DueCountDTO(dueToday, newCards);
    }

    private Optional<SessionCard> findFirstUnreviewed(Long sessionId) {
        List<SessionCard> cards = cardRepo.findFirstUnreviewed(sessionId, PageRequest.of(0, 1));
        return cards.isEmpty() ? Optional.empty() : Optional.of(cards.get(0));
    }

    private StudyCardDTO toCardDTO(Lemma lemma, Long sessionId) {
        long remaining = sessionRepo.findById(sessionId)
            .map(s -> (long)(s.getCardCount() - s.getCardsReviewed()))
            .orElse(0L);
        return new StudyCardDTO(lemma.getId(), lemma.getText(), lemma.getTranslation(), remaining);
    }
}
