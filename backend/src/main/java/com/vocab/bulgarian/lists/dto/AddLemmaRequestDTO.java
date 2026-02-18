package com.vocab.bulgarian.lists.dto;

import jakarta.validation.constraints.NotNull;

public record AddLemmaRequestDTO(@NotNull Long lemmaId) {}
