package com.vocab.bulgarian.study.repository;

import com.vocab.bulgarian.study.domain.SessionCard;
import com.vocab.bulgarian.study.domain.SessionCardId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionCardRepository extends JpaRepository<SessionCard, SessionCardId> {

    @Query("""
        SELECT sc FROM SessionCard sc
        JOIN FETCH sc.lemma
        WHERE sc.session.id = :sessionId
        AND sc.reviewed = false
        ORDER BY sc.position ASC
        """)
    List<SessionCard> findFirstUnreviewed(@Param("sessionId") Long sessionId, Pageable pageable);
}
