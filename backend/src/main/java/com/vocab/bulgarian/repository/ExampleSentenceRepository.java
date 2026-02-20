package com.vocab.bulgarian.repository;

import com.vocab.bulgarian.domain.ExampleSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExampleSentenceRepository extends JpaRepository<ExampleSentence, Long> {

    List<ExampleSentence> findByLemmaIdOrderBySortOrderAsc(Long lemmaId);

    void deleteAllByLemmaId(Long lemmaId);

    long countByLemmaId(Long lemmaId);
}
