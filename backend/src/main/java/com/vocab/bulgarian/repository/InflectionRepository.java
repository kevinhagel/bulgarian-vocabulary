package com.vocab.bulgarian.repository;

import com.vocab.bulgarian.domain.Inflection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Inflection entities.
 * Provides derived queries and PGroonga full-text search on inflected forms.
 */
@Repository
public interface InflectionRepository extends JpaRepository<Inflection, Long> {

    // Derived query methods
    List<Inflection> findByLemmaId(Long lemmaId);

    List<Inflection> findByLemmaIdOrderByFormAsc(Long lemmaId);

    boolean existsByFormAndLemmaId(String form, Long lemmaId);

    // PGroonga native search query for Cyrillic full-text search on inflected forms
    @Query(value = "SELECT * FROM inflections WHERE form &@~ :searchQuery LIMIT 20", nativeQuery = true)
    List<Inflection> searchByForm(@Param("searchQuery") String searchQuery);
}
