package com.vocab.bulgarian.dictionary.repository;

import com.vocab.bulgarian.dictionary.domain.DictionaryForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DictionaryFormRepository extends JpaRepository<DictionaryForm, Long> {

    List<DictionaryForm> findByPlainForm(String plainForm);

    @Query(value = "SELECT * FROM dictionary_forms WHERE plain_form &@~ :query ORDER BY plain_form LIMIT 50",
           nativeQuery = true)
    List<DictionaryForm> searchByPlainForm(@Param("query") String query);

    @Query("SELECT f FROM DictionaryForm f JOIN FETCH f.dictionaryWord WHERE f.plainForm = :plainForm")
    List<DictionaryForm> findByPlainFormWithWord(@Param("plainForm") String plainForm);

    List<DictionaryForm> findByDictionaryWordId(Long wordId);
}
