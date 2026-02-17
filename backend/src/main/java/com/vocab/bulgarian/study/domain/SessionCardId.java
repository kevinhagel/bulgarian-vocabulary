package com.vocab.bulgarian.study.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SessionCardId implements Serializable {
    private Long sessionId;
    private Long lemmaId;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getLemmaId() { return lemmaId; }
    public void setLemmaId(Long lemmaId) { this.lemmaId = lemmaId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionCardId that)) return false;
        return Objects.equals(sessionId, that.sessionId) && Objects.equals(lemmaId, that.lemmaId);
    }

    @Override
    public int hashCode() { return Objects.hash(sessionId, lemmaId); }
}
