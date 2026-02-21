package com.vocab.bulgarian.service;

import com.vocab.bulgarian.api.dto.AdminStatsDTO;
import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.domain.enums.ProcessingStatus;
import com.vocab.bulgarian.domain.enums.ReviewStatus;
import com.vocab.bulgarian.domain.enums.SentenceStatus;
import com.vocab.bulgarian.repository.LemmaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final LemmaRepository lemmaRepository;
    private final SentenceService sentenceService;

    public AdminService(LemmaRepository lemmaRepository, SentenceService sentenceService) {
        this.lemmaRepository = lemmaRepository;
        this.sentenceService = sentenceService;
    }

    public AdminStatsDTO getStats() {
        // Lemma stats
        long total       = lemmaRepository.count();
        long completed   = lemmaRepository.countByProcessingStatus(ProcessingStatus.COMPLETED);
        long failed      = lemmaRepository.countByProcessingStatus(ProcessingStatus.FAILED);
        long processing  = lemmaRepository.countByProcessingStatus(ProcessingStatus.PROCESSING);
        long queued      = lemmaRepository.countByProcessingStatus(ProcessingStatus.QUEUED);
        long reviewed    = lemmaRepository.countByReviewStatus(ReviewStatus.REVIEWED);
        long pending     = lemmaRepository.countByReviewStatus(ReviewStatus.PENDING);
        long needsCorrection = lemmaRepository.countByReviewStatus(ReviewStatus.NEEDS_CORRECTION);

        var lemmaStats = new AdminStatsDTO.LemmaStats(
                total, completed, failed, processing, queued, reviewed, pending, needsCorrection);

        // Sentence stats
        long sentDone       = lemmaRepository.countBySentenceStatus(SentenceStatus.DONE);
        long sentNone       = lemmaRepository.countBySentenceStatus(SentenceStatus.NONE);
        long sentQueued     = lemmaRepository.countBySentenceStatus(SentenceStatus.QUEUED);
        long sentGenerating = lemmaRepository.countBySentenceStatus(SentenceStatus.GENERATING);
        long sentFailed     = lemmaRepository.countBySentenceStatus(SentenceStatus.FAILED);

        var sentenceStats = new AdminStatsDTO.SentenceStats(
                sentDone, sentNone, sentQueued, sentGenerating, sentFailed);

        long totalInflections = lemmaRepository.countAllInflections();

        // Failed lemmas (inflection/processing failures)
        List<AdminStatsDTO.IssueLemmaDTO> failedLemmas = lemmaRepository.findFailedLemmas()
                .stream().map(this::toIssueLemmaDTO).toList();

        // Stuck lemmas: PROCESSING for more than 15 minutes
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<AdminStatsDTO.IssueLemmaDTO> stuckLemmas = lemmaRepository.findStuckLemmas(cutoff)
                .stream().map(this::toIssueLemmaDTO).toList();

        // Duplicates
        List<AdminStatsDTO.DuplicateGroupDTO> duplicates = buildDuplicateGroups(
                lemmaRepository.findDuplicateRows());

        // Failed sentence generation
        List<AdminStatsDTO.FailedSentenceDTO> failedSentences = lemmaRepository
                .findBySentenceStatusIn(List.of(SentenceStatus.FAILED))
                .stream()
                .map(l -> new AdminStatsDTO.FailedSentenceDTO(l.getId(), l.getText()))
                .toList();

        double avgSentenceSeconds = sentenceService.getAvgSentenceGenerationSeconds();

        return new AdminStatsDTO(lemmaStats, sentenceStats, totalInflections,
                failedLemmas, stuckLemmas, duplicates, failedSentences, avgSentenceSeconds);
    }

    private AdminStatsDTO.IssueLemmaDTO toIssueLemmaDTO(Lemma l) {
        return new AdminStatsDTO.IssueLemmaDTO(
                l.getId(),
                l.getText(),
                l.getNotes(),
                l.getProcessingStatus().name(),
                l.getProcessingError(),
                l.getUpdatedAt() != null ? l.getUpdatedAt().toString() : null
        );
    }

    private List<AdminStatsDTO.DuplicateGroupDTO> buildDuplicateGroups(List<Object[]> rows) {
        Map<String, AdminStatsDTO.DuplicateGroupDTO> groups = new LinkedHashMap<>();
        for (Object[] row : rows) {
            long id = ((Number) row[0]).longValue();
            String text = (String) row[1];
            String source = (String) row[2];
            String notes = (String) row[3];
            String processingStatus = (String) row[4];
            String createdAt = row[5] != null ? row[5].toString() : null;

            String key = text + "|" + source;
            AdminStatsDTO.DuplicateGroupDTO group = groups.get(key);
            if (group == null) {
                group = new AdminStatsDTO.DuplicateGroupDTO(text, source, new ArrayList<>());
                groups.put(key, group);
            }
            group.entries().add(new AdminStatsDTO.DuplicateEntryDTO(id, notes, processingStatus, createdAt));
        }
        return new ArrayList<>(groups.values());
    }

    @CacheEvict(value = {"lemma", "lemmaList"}, allEntries = true)
    @Transactional
    public void clearCache() {
        // Method body intentionally empty — @CacheEvict handles the work
    }
}
