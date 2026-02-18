package com.vocab.bulgarian.lists.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameWordListRequestDTO(
    @NotBlank @Size(min = 1, max = 100) String name
) {}
