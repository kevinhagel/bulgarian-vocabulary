package com.vocab.bulgarian.study.dto;

public record ProgressDashboardDTO(
    long totalUserVocab,
    long totalVocabStudied,
    long totalSessions,
    long totalCardsReviewed,
    long totalCorrect,
    int retentionRate,
    long cardsDueToday,
    long newCards
) {}
