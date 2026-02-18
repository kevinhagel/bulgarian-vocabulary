package com.vocab.bulgarian.study.repository;

import com.vocab.bulgarian.study.domain.SrsState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SrsStateRepository extends JpaRepository<SrsState, Long> {

    Optional<SrsState> findByLemmaId(Long lemmaId);

    @Query("SELECT s FROM SrsState s WHERE s.nextReviewDate <= :today AND s.lemma.reviewStatus = com.vocab.bulgarian.domain.enums.ReviewStatus.REVIEWED")
    List<SrsState> findDueCards(@Param("today") LocalDate today);

    @Query("SELECT COUNT(s) FROM SrsState s WHERE s.nextReviewDate <= :today AND s.lemma.reviewStatus = com.vocab.bulgarian.domain.enums.ReviewStatus.REVIEWED")
    long countDueCards(@Param("today") LocalDate today);

    @Query("""
        SELECT l.id FROM Lemma l
        WHERE l.source = com.vocab.bulgarian.domain.enums.Source.USER_ENTERED
        AND l.processingStatus = com.vocab.bulgarian.domain.enums.ProcessingStatus.COMPLETED
        AND l.reviewStatus = com.vocab.bulgarian.domain.enums.ReviewStatus.REVIEWED
        AND l.id NOT IN (SELECT s.lemma.id FROM SrsState s)
        """)
    List<Long> findLemmaIdsWithoutSrsState();
}
