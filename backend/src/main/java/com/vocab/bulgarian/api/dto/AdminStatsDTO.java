package com.vocab.bulgarian.api.dto;

import java.util.List;

public record AdminStatsDTO(
    LemmaStats lemmas,
    SentenceStats sentences,
    long totalInflections,
    List<IssueLemmaDTO> failedLemmas,
    List<IssueLemmaDTO> stuckLemmas,
    List<DuplicateGroupDTO> duplicates
) {
    public record LemmaStats(
        long total,
        long completed,
        long failed,
        long processing,
        long queued,
        long reviewed,
        long pending,
        long needsCorrection
    ) {}

    public record SentenceStats(
        long done,
        long none,
        long queued,
        long generating,
        long failed
    ) {}

    public record IssueLemmaDTO(
        long id,
        String text,
        String notes,
        String processingStatus,
        String errorMessage,
        String updatedAt
    ) {}

    public record DuplicateGroupDTO(
        String text,
        String source,
        List<DuplicateEntryDTO> entries
    ) {}

    public record DuplicateEntryDTO(
        long id,
        String notes,
        String processingStatus,
        String createdAt
    ) {}
}
