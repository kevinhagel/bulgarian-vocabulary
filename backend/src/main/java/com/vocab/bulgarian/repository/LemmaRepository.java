package com.vocab.bulgarian.repository;

import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.domain.enums.DifficultyLevel;
import com.vocab.bulgarian.domain.enums.PartOfSpeech;
import com.vocab.bulgarian.domain.enums.ReviewStatus;
import com.vocab.bulgarian.domain.enums.Source;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Lemma entities.
 * Provides derived queries, JPQL queries with JOIN FETCH, and PGroonga full-text search.
 */
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    // Derived query methods
    List<Lemma> findBySourceOrderByTextAsc(Source source);

    List<Lemma> findByPartOfSpeechOrderByTextAsc(PartOfSpeech partOfSpeech);

    List<Lemma> findByDifficultyLevelOrderByTextAsc(DifficultyLevel difficultyLevel);

    boolean existsByTextAndSource(String text, Source source);

    Optional<Lemma> findByTextIgnoreCase(String text);

    List<Lemma> findByReviewStatus(ReviewStatus reviewStatus);

    // Custom JPQL queries with JOIN FETCH to solve N+1 problem
    @Query("SELECT l FROM Lemma l LEFT JOIN FETCH l.inflections WHERE l.id = :id")
    Optional<Lemma> findByIdWithInflections(@Param("id") Long id);

    @Query("SELECT DISTINCT l FROM Lemma l LEFT JOIN FETCH l.inflections ORDER BY l.text ASC")
    List<Lemma> findAllWithInflections();

    @Query("SELECT DISTINCT l FROM Lemma l LEFT JOIN FETCH l.inflections WHERE l.source = :source ORDER BY l.text ASC")
    List<Lemma> findBySourceWithInflections(@Param("source") Source source);

    // PGroonga native search query for Cyrillic full-text search
    @Query(value = "SELECT * FROM lemmas WHERE text &@~ :searchQuery ORDER BY pgroonga_score(tableoid, ctid) DESC LIMIT 20", nativeQuery = true)
    List<Lemma> searchByText(@Param("searchQuery") String searchQuery);

    // Paginated browse with optional filtering by source
    Page<Lemma> findBySource(Source source, Pageable pageable);

    // Paginated browse with optional filtering by part of speech
    Page<Lemma> findByPartOfSpeech(PartOfSpeech partOfSpeech, Pageable pageable);

    // Paginated browse with optional filtering by difficulty level
    Page<Lemma> findByDifficultyLevel(DifficultyLevel difficultyLevel, Pageable pageable);

    // Paginated browse with combined filters (source + partOfSpeech)
    Page<Lemma> findBySourceAndPartOfSpeech(Source source, PartOfSpeech partOfSpeech, Pageable pageable);

    // Paginated browse with combined filters (source + difficultyLevel)
    Page<Lemma> findBySourceAndDifficultyLevel(Source source, DifficultyLevel difficultyLevel, Pageable pageable);

    // Paginated browse with combined filters (partOfSpeech + difficultyLevel)
    Page<Lemma> findByPartOfSpeechAndDifficultyLevel(PartOfSpeech partOfSpeech, DifficultyLevel difficultyLevel, Pageable pageable);

    // Paginated browse with all three filters
    Page<Lemma> findBySourceAndPartOfSpeechAndDifficultyLevel(Source source, PartOfSpeech partOfSpeech, DifficultyLevel difficultyLevel, Pageable pageable);

    // Batch load inflections for list of IDs (after pagination)
    @Query("SELECT DISTINCT l FROM Lemma l LEFT JOIN FETCH l.inflections WHERE l.id IN :ids")
    List<Lemma> findByIdInWithInflections(@Param("ids") List<Long> ids);
}
