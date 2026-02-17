package com.vocab.bulgarian.study.domain;

import com.vocab.bulgarian.domain.Lemma;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "srs_state")
public class SrsState {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false, unique = true)
    private Lemma lemma;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor = new BigDecimal("2.50");

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 0;

    @Column(name = "repetition_count", nullable = false)
    private int repetitionCount = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate = LocalDate.now();

    @Column(name = "last_reviewed_at")
    private ZonedDateTime lastReviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = ZonedDateTime.now(); }

    public Long getId() { return id; }
    public Lemma getLemma() { return lemma; }
    public void setLemma(Lemma lemma) { this.lemma = lemma; }
    public BigDecimal getEaseFactor() { return easeFactor; }
    public void setEaseFactor(BigDecimal easeFactor) { this.easeFactor = easeFactor; }
    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }
    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }
    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public ZonedDateTime getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(ZonedDateTime lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }
}
