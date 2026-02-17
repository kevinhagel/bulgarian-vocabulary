package com.vocab.bulgarian.study.dto;

public record SessionSummaryDTO(Long sessionId, String status, int cardCount, int cardsReviewed, int correctCount, int retentionRate) {}
