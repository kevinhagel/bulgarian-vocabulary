package com.vocab.bulgarian.lists.repository;

import com.vocab.bulgarian.lists.domain.WordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordListRepository extends JpaRepository<WordList, Long> {

    @Query("SELECT wl FROM WordList wl ORDER BY wl.name ASC")
    List<WordList> findAllOrderByName();

    @Query("SELECT wl FROM WordList wl LEFT JOIN FETCH wl.lemmas WHERE wl.id = :id")
    Optional<WordList> findByIdWithLemmas(@Param("id") Long id);

    @Query("SELECT SIZE(wl.lemmas) FROM WordList wl WHERE wl.id = :id")
    int countLemmasByListId(@Param("id") Long id);

    @Query("SELECT l.id FROM WordList wl JOIN wl.lemmas l WHERE wl.id = :listId")
    List<Long> findLemmaIdsByListId(@Param("listId") Long listId);

    @Modifying
    @Query(value = "INSERT INTO word_list_members(list_id, lemma_id) VALUES (:listId, :lemmaId) ON CONFLICT DO NOTHING",
           nativeQuery = true)
    void addMember(@Param("listId") Long listId, @Param("lemmaId") Long lemmaId);

    @Modifying
    @Query(value = "DELETE FROM word_list_members WHERE list_id = :listId AND lemma_id = :lemmaId",
           nativeQuery = true)
    void removeMember(@Param("listId") Long listId, @Param("lemmaId") Long lemmaId);
}
