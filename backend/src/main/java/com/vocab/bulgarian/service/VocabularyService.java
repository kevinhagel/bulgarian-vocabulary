package com.vocab.bulgarian.service;

import com.vocab.bulgarian.api.dto.*;
import com.vocab.bulgarian.api.mapper.LemmaMapper;
import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.domain.enums.DifficultyLevel;
import com.vocab.bulgarian.domain.enums.PartOfSpeech;
import com.vocab.bulgarian.domain.enums.ProcessingStatus;
import com.vocab.bulgarian.domain.enums.ReviewStatus;
import com.vocab.bulgarian.domain.enums.Source;
import com.vocab.bulgarian.llm.service.LlmOrchestrationService;
import com.vocab.bulgarian.repository.LemmaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for vocabulary management operations.
 * Integrates LLM orchestration for create flow and provides CRUD operations.
 */
@Service
@Transactional(readOnly = true)
public class VocabularyService {

    private final LemmaRepository lemmaRepository;
    private final LlmOrchestrationService llmOrchestrationService;
    private final BackgroundProcessingService backgroundProcessingService;
    private final LemmaMapper lemmaMapper;

    public VocabularyService(
        LemmaRepository lemmaRepository,
        LlmOrchestrationService llmOrchestrationService,
        BackgroundProcessingService backgroundProcessingService,
        LemmaMapper lemmaMapper
    ) {
        this.lemmaRepository = lemmaRepository;
        this.llmOrchestrationService = llmOrchestrationService;
        this.backgroundProcessingService = backgroundProcessingService;
        this.lemmaMapper = lemmaMapper;
    }

    /**
     * Create new vocabulary entry with background LLM processing.
     * Saves entry immediately with QUEUED status, then triggers async processing.
     * Returns instantly (< 1 second) instead of blocking for LLM (60-90 seconds).
     *
     * @param request create request with word form, optional translation, and notes
     * @return CompletableFuture with created lemma detail DTO (status: QUEUED)
     */
    @Transactional
    public CompletableFuture<LemmaDetailDTO> createVocabulary(CreateLemmaRequestDTO request) {
        // Step 1: Create lemma entity with user input (translation optional)
        Lemma lemma = new Lemma();
        lemma.setText(request.wordForm().trim().toLowerCase()); // Will be updated to canonical form during background processing
        lemma.setTranslation(request.translation()); // May be null - auto-translated in background
        lemma.setNotes(request.notes());
        lemma.setSource(Source.USER_ENTERED);
        lemma.setReviewStatus(ReviewStatus.PENDING);
        lemma.setProcessingStatus(ProcessingStatus.QUEUED);

        // Step 2: Save immediately (returns in < 1 second)
        Lemma saved = lemmaRepository.save(lemma);

        // Step 3: Trigger background processing after TX commits (lemma must be visible to async thread)
        Long lemmaId = saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                backgroundProcessingService.processLemma(lemmaId);
            }
        });

        // Step 4: Return saved entry with QUEUED status
        return CompletableFuture.completedFuture(lemmaMapper.toDetailDTO(saved));
    }

    /**
     * Get vocabulary entry by ID with full detail including inflections.
     *
     * @param id lemma ID
     * @return lemma detail DTO
     * @throws EntityNotFoundException if lemma not found
     */
    public LemmaDetailDTO getVocabularyById(Long id) {
        Lemma lemma = lemmaRepository.findByIdWithInflections(id)
            .orElseThrow(() -> new EntityNotFoundException("Lemma not found with id: " + id));
        return lemmaMapper.toDetailDTO(lemma);
    }

    /**
     * Browse vocabulary with optional filtering and pagination.
     * Supports filtering by source, part of speech, and difficulty level.
     * Note: inflectionCount in response will be 0 for paginated queries
     * since inflections are lazy-loaded. This is acceptable for list view.
     *
     * @param source optional source filter
     * @param partOfSpeech optional part of speech filter
     * @param difficultyLevel optional difficulty level filter
     * @param pageable pagination parameters
     * @return page of lemma summary DTOs
     */
    public Page<LemmaResponseDTO> browseVocabulary(
        Source source,
        PartOfSpeech partOfSpeech,
        DifficultyLevel difficultyLevel,
        Pageable pageable
    ) {
        Page<Lemma> page;

        // Build query based on which filters are present
        if (source != null && partOfSpeech != null && difficultyLevel != null) {
            page = lemmaRepository.findBySourceAndPartOfSpeechAndDifficultyLevel(source, partOfSpeech, difficultyLevel, pageable);
        } else if (source != null && partOfSpeech != null) {
            page = lemmaRepository.findBySourceAndPartOfSpeech(source, partOfSpeech, pageable);
        } else if (source != null && difficultyLevel != null) {
            page = lemmaRepository.findBySourceAndDifficultyLevel(source, difficultyLevel, pageable);
        } else if (partOfSpeech != null && difficultyLevel != null) {
            page = lemmaRepository.findByPartOfSpeechAndDifficultyLevel(partOfSpeech, difficultyLevel, pageable);
        } else if (source != null) {
            page = lemmaRepository.findBySource(source, pageable);
        } else if (partOfSpeech != null) {
            page = lemmaRepository.findByPartOfSpeech(partOfSpeech, pageable);
        } else if (difficultyLevel != null) {
            page = lemmaRepository.findByDifficultyLevel(difficultyLevel, pageable);
        } else {
            page = lemmaRepository.findAll(pageable);
        }

        return page.map(lemmaMapper::toResponseDTO);
    }

    /**
     * Search vocabulary using PGroonga full-text search for Cyrillic text.
     * Returns up to 20 results ordered by relevance score.
     * Note: inflectionCount will be 0 since native query doesn't load inflections.
     *
     * @param query search query
     * @return list of matching lemma summary DTOs
     */
    public List<LemmaResponseDTO> searchVocabulary(String query) {
        // Search lemma text first (ordered by PGroonga relevance score)
        List<Lemma> byText = lemmaRepository.searchByText(query);

        // Also search inflected forms and include their parent lemmas
        List<Lemma> byInflection = lemmaRepository.searchByInflectionForm(query);

        // Merge: lemma-text matches first, then inflection-only matches (deduplicated by ID)
        var seen = new java.util.LinkedHashSet<Long>();
        var merged = new java.util.ArrayList<Lemma>(byText.size() + byInflection.size());
        for (Lemma l : byText) {
            if (seen.add(l.getId())) merged.add(l);
        }
        for (Lemma l : byInflection) {
            if (seen.add(l.getId())) merged.add(l);
        }

        return merged.stream()
            .map(lemmaMapper::toResponseDTO)
            .toList();
    }

    /**
     * Update existing vocabulary entry.
     * Sets review status to PENDING for re-review after edit.
     *
     * @param id lemma ID
     * @param request update request
     * @return updated lemma detail DTO
     * @throws EntityNotFoundException if lemma not found
     */
    @Transactional
    public LemmaDetailDTO updateVocabulary(Long id, UpdateLemmaRequestDTO request) {
        Lemma lemma = lemmaRepository.findByIdWithInflections(id)
            .orElseThrow(() -> new EntityNotFoundException("Lemma not found with id: " + id));

        lemmaMapper.updateEntity(request, lemma);
        lemma.setReviewStatus(ReviewStatus.PENDING);

        Lemma saved = lemmaRepository.save(lemma);
        return lemmaMapper.toDetailDTO(saved);
    }

    /**
     * Delete vocabulary entry.
     * Cascades to delete all associated inflections.
     *
     * @param id lemma ID
     * @throws EntityNotFoundException if lemma not found
     */
    @Transactional
    public void deleteVocabulary(Long id) {
        if (!lemmaRepository.existsById(id)) {
            throw new EntityNotFoundException("Lemma not found with id: " + id);
        }
        lemmaRepository.deleteById(id);
    }

    /**
     * Update review status for a vocabulary entry.
     *
     * @param id lemma ID
     * @param status new review status
     * @return updated lemma detail DTO
     * @throws EntityNotFoundException if lemma not found
     */
    @Transactional
    public LemmaDetailDTO updateReviewStatus(Long id, ReviewStatus status) {
        Lemma lemma = lemmaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lemma not found with id: " + id));

        lemma.setReviewStatus(status);
        lemmaRepository.save(lemma);

        // Reload with inflections for response
        return lemmaMapper.toDetailDTO(
            lemmaRepository.findByIdWithInflections(id)
                .orElseThrow(() -> new EntityNotFoundException("Lemma not found with id: " + id))
        );
    }

    /**
     * Reprocess a vocabulary entry through the LLM pipeline.
     * Clears existing inflections, resets processing status, and appends
     * an optional disambiguation hint to the notes before re-queuing.
     *
     * @param id lemma ID
     * @param request optional hint to help the LLM disambiguate
     * @return updated lemma detail DTO (status: QUEUED)
     * @throws EntityNotFoundException if lemma not found
     */
    @Transactional
    public LemmaDetailDTO reprocessVocabulary(Long id, ReprocessRequestDTO request) {
        Lemma lemma = lemmaRepository.findByIdWithInflections(id)
            .orElseThrow(() -> new EntityNotFoundException("Lemma not found with id: " + id));

        // Append hint to notes so background processor sees it
        if (request != null && request.hint() != null && !request.hint().isBlank()) {
            String existing = lemma.getNotes() != null ? lemma.getNotes().trim() : "";
            String appended = existing.isEmpty()
                ? request.hint().trim()
                : existing + "; " + request.hint().trim();
            lemma.setNotes(appended);
        }

        lemma.getInflections().clear();
        lemma.setProcessingStatus(ProcessingStatus.QUEUED);
        lemma.setProcessingError(null);
        lemma.setReviewStatus(ReviewStatus.PENDING);

        Lemma saved = lemmaRepository.save(lemma);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                backgroundProcessingService.processLemma(saved.getId());
            }
        });

        return lemmaMapper.toDetailDTO(saved);
    }

    /**
     * Flag a vocabulary entry as needing correction.
     * Sets review status to NEEDS_CORRECTION so it appears in the review queue.
     *
     * @param id lemma ID
     * @return updated lemma detail DTO
     * @throws EntityNotFoundException if lemma not found
     */
    @Transactional
    public LemmaDetailDTO flagVocabulary(Long id) {
        Lemma lemma = lemmaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lemma not found with id: " + id));

        lemma.setReviewStatus(ReviewStatus.NEEDS_CORRECTION);
        lemmaRepository.save(lemma);

        return lemmaMapper.toDetailDTO(
            lemmaRepository.findByIdWithInflections(id)
                .orElseThrow(() -> new EntityNotFoundException("Lemma not found with id: " + id))
        );
    }

    /**
     * Get the review queue: all user-entered words that are PENDING or NEEDS_CORRECTION.
     *
     * @param pageable pagination parameters
     * @return page of lemma summary DTOs needing review
     */
    public Page<LemmaResponseDTO> getReviewQueue(Pageable pageable) {
        List<ReviewStatus> statuses = List.of(ReviewStatus.PENDING, ReviewStatus.NEEDS_CORRECTION);
        return lemmaRepository.findReviewQueue(statuses, pageable)
            .map(lemmaMapper::toResponseDTO);
    }
}
