package com.vocab.bulgarian.study.domain;

import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.study.domain.enums.ReviewRating;
import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "study_reviews")
public class StudyReview {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewRating rating;

    @Column(name = "reviewed_at", nullable = false)
    private ZonedDateTime reviewedAt = ZonedDateTime.now();

    public Long getId() { return id; }
    public StudySession getSession() { return session; }
    public void setSession(StudySession session) { this.session = session; }
    public Lemma getLemma() { return lemma; }
    public void setLemma(Lemma lemma) { this.lemma = lemma; }
    public ReviewRating getRating() { return rating; }
    public void setRating(ReviewRating rating) { this.rating = rating; }
    public ZonedDateTime getReviewedAt() { return reviewedAt; }
}
