package com.vocab.bulgarian.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating or creating inflections as part of lemma update.
 * If id is null, this represents a new inflection to be created.
 * If id is present, this represents an existing inflection to update.
 */
public record InflectionUpdateDTO(
    Long id,

    @NotBlank
    @Size(min = 1, max = 100)
    String form,

    @Size(max = 100)
    String grammaticalInfo
) {
}
