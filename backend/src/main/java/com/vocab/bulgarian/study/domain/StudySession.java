package com.vocab.bulgarian.study.domain;

import com.vocab.bulgarian.study.domain.enums.SessionStatus;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "study_sessions")
public class StudySession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "card_count", nullable = false)
    private int cardCount = 0;

    @Column(name = "cards_reviewed", nullable = false)
    private int cardsReviewed = 0;

    @Column(name = "correct_count", nullable = false)
    private int correctCount = 0;

    @Column(name = "started_at", nullable = false, updatable = false)
    private ZonedDateTime startedAt = ZonedDateTime.now();

    @Column(name = "ended_at")
    private ZonedDateTime endedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<SessionCard> cards = new ArrayList<>();

    public Long getId() { return id; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public int getCardCount() { return cardCount; }
    public void setCardCount(int cardCount) { this.cardCount = cardCount; }
    public int getCardsReviewed() { return cardsReviewed; }
    public void setCardsReviewed(int cardsReviewed) { this.cardsReviewed = cardsReviewed; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public ZonedDateTime getStartedAt() { return startedAt; }
    public ZonedDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(ZonedDateTime endedAt) { this.endedAt = endedAt; }
    public List<SessionCard> getCards() { return cards; }
}
