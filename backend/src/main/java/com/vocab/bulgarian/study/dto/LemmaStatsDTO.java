package com.vocab.bulgarian.study.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record LemmaStatsDTO(
    Long lemmaId,
    long reviewCount,
    long correctCount,
    int correctRate,
    ZonedDateTime lastReviewedAt,
    LocalDate nextReviewDate,
    int intervalDays,
    double easeFactor
) {}
