package com.vocab.bulgarian.dictionary.repository;

import com.vocab.bulgarian.dictionary.domain.DictionaryWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DictionaryWordRepository extends JpaRepository<DictionaryWord, Long> {

    List<DictionaryWord> findByWordAndPos(String word, String pos);

    List<DictionaryWord> findByWord(String word);

    @Query(value = "SELECT * FROM dictionary_words WHERE word &@~ :query ORDER BY word LIMIT 20",
           nativeQuery = true)
    List<DictionaryWord> searchByWord(@Param("query") String query);

    long count();
}
