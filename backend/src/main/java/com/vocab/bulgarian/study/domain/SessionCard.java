package com.vocab.bulgarian.study.domain;

import com.vocab.bulgarian.domain.Lemma;
import jakarta.persistence.*;

@Entity
@Table(name = "session_cards")
public class SessionCard {

    @EmbeddedId
    private SessionCardId id = new SessionCardId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sessionId")
    @JoinColumn(name = "session_id")
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lemmaId")
    @JoinColumn(name = "lemma_id")
    private Lemma lemma;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean reviewed = false;

    public SessionCardId getId() { return id; }
    public StudySession getSession() { return session; }
    public void setSession(StudySession session) { this.session = session; }
    public Lemma getLemma() { return lemma; }
    public void setLemma(Lemma lemma) { this.lemma = lemma; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
}
