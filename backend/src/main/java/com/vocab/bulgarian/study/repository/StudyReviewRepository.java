package com.vocab.bulgarian.study.repository;

import com.vocab.bulgarian.study.domain.StudyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyReviewRepository extends JpaRepository<StudyReview, Long> {

    long countByLemmaId(Long lemmaId);

    @Query("SELECT COUNT(r) FROM StudyReview r WHERE r.lemma.id = :lemmaId AND r.rating = com.vocab.bulgarian.study.domain.enums.ReviewRating.CORRECT")
    long countCorrectByLemmaId(@Param("lemmaId") Long lemmaId);
}
