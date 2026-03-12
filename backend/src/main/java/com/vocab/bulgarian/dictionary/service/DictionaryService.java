package com.vocab.bulgarian.dictionary.service;

import com.vocab.bulgarian.dictionary.domain.DictionaryForm;
import com.vocab.bulgarian.dictionary.domain.DictionaryWord;
import com.vocab.bulgarian.dictionary.dto.DictionaryFormDTO;
import com.vocab.bulgarian.dictionary.dto.DictionarySearchResultDTO;
import com.vocab.bulgarian.dictionary.repository.DictionaryFormRepository;
import com.vocab.bulgarian.dictionary.repository.DictionaryWordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DictionaryService {

    private static final Logger log = LoggerFactory.getLogger(DictionaryService.class);

    private final DictionaryWordRepository wordRepository;
    private final DictionaryFormRepository formRepository;

    public DictionaryService(DictionaryWordRepository wordRepository,
                             DictionaryFormRepository formRepository) {
        this.wordRepository = wordRepository;
        this.formRepository = formRepository;
    }

    /**
     * Search dictionary by any form (inflected or canonical).
     * Strips accent marks from query, searches plain_form, returns parent dictionary words.
     */
    public List<DictionarySearchResultDTO> searchByForm(String query) {
        String plainQuery = stripAccents(query.trim().toLowerCase());

        // First try exact match on plain_form
        List<DictionaryForm> forms = formRepository.findByPlainFormWithWord(plainQuery);

        // Also try exact match on dictionary_words.word
        List<DictionaryWord> directMatches = wordRepository.findByWord(plainQuery);

        // Merge results: collect unique dictionary words
        Map<Long, DictionaryWord> wordMap = new LinkedHashMap<>();
        for (DictionaryForm f : forms) {
            wordMap.putIfAbsent(f.getDictionaryWord().getId(), f.getDictionaryWord());
        }
        for (DictionaryWord w : directMatches) {
            wordMap.putIfAbsent(w.getId(), w);
        }

        return wordMap.values().stream()
            .map(this::toSearchResult)
            .toList();
    }

    /**
     * Look up a specific dictionary word by ID with all its forms.
     */
    public Optional<DictionarySearchResultDTO> getById(Long dictionaryWordId) {
        return wordRepository.findById(dictionaryWordId)
            .map(this::toSearchResult);
    }

    /**
     * Load the dictionary word entity by ID.
     */
    public Optional<DictionaryWord> findWordById(Long dictionaryWordId) {
        return wordRepository.findById(dictionaryWordId);
    }

    /**
     * Look up dictionary word by exact word and pos.
     */
    public Optional<DictionaryWord> lookupByWordAndPos(String word, String pos) {
        List<DictionaryWord> matches = wordRepository.findByWordAndPos(word, pos);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getFirst());
    }

    /**
     * Get all forms for a dictionary word.
     */
    public List<DictionaryForm> getFormsForWord(Long dictionaryWordId) {
        return formRepository.findByDictionaryWordId(dictionaryWordId);
    }

    /**
     * Strip Unicode combining acute accent (U+0301) and return plain text.
     */
    public static String stripAccents(String text) {
        if (text == null) return null;
        // Decompose to NFD so combining acute (U+0301) is separate, then remove it
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\u0301", "");
    }

    private DictionarySearchResultDTO toSearchResult(DictionaryWord word) {
        List<DictionaryForm> wordForms = formRepository.findByDictionaryWordId(word.getId());

        List<DictionaryFormDTO> formDtos = wordForms.stream()
            .filter(f -> f.getTags() != null && !isMetaTag(f.getTags()))
            .map(f -> new DictionaryFormDTO(
                f.getForm(),
                f.getPlainForm(),
                Arrays.asList(f.getTags()),
                f.getAccentedForm(),
                f.getRomanization()
            ))
            .toList();

        return new DictionarySearchResultDTO(
            word.getId(),
            word.getWord(),
            word.getPos(),
            word.getPrimaryTranslation(),
            word.getAlternateMeanings() != null
                ? Arrays.asList(word.getAlternateMeanings())
                : List.of(),
            word.getIpa(),
            formDtos
        );
    }

    /**
     * Filter out meta tags like "romanization", "table-tags", "inflection-template"
     * that aren't actual inflected forms.
     */
    private boolean isMetaTag(String[] tags) {
        for (String tag : tags) {
            if ("romanization".equals(tag) || "table-tags".equals(tag) || "inflection-template".equals(tag)) {
                return true;
            }
        }
        return false;
    }
}
