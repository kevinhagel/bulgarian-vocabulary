package com.vocab.bulgarian.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for updating an existing lemma.
 * Allows updating lemma text, translation, notes, and optionally the inflections list.
 */
public record UpdateLemmaRequestDTO(
    @NotBlank(groups = OnUpdate.class)
    @Size(min = 1, max = 100)
    String text,

    @NotBlank(groups = OnUpdate.class)
    @Size(min = 1, max = 200)
    String translation,

    @Size(max = 5000)
    String notes,

    List<InflectionUpdateDTO> inflections
) {
}
