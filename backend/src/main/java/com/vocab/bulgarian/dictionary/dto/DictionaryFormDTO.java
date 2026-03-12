package com.vocab.bulgarian.dictionary.dto;

import java.util.List;

public record DictionaryFormDTO(
    String form,
    String plainForm,
    List<String> tags,
    String accentedForm,
    String romanization
) {}
