package com.vocab.bulgarian.study.repository;

import com.vocab.bulgarian.study.domain.StudySession;
import com.vocab.bulgarian.study.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    long countByStatus(SessionStatus status);

    @Query("SELECT COALESCE(SUM(s.cardsReviewed), 0) FROM StudySession s")
    long sumTotalCardsReviewed();

    @Query("SELECT COALESCE(SUM(s.correctCount), 0) FROM StudySession s")
    long sumTotalCorrect();
}
