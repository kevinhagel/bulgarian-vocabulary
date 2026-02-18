package com.vocab.bulgarian.lists.dto;

import java.time.ZonedDateTime;

public record WordListSummaryDTO(Long id, String name, int lemmaCount, ZonedDateTime createdAt) {}
