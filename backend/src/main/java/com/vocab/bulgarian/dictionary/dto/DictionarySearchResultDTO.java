package com.vocab.bulgarian.dictionary.dto;

import java.util.List;

public record DictionarySearchResultDTO(
    Long dictionaryWordId,
    String word,
    String pos,
    String primaryTranslation,
    List<String> alternateMeanings,
    String ipa,
    List<DictionaryFormDTO> forms
) {}
