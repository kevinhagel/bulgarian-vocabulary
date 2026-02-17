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
        lemma.setText(request.wordForm()); // Will be updated to canonical form during background processing
        lemma.setTranslation(request.translation()); // May be null - auto-translated in background
        lemma.setNotes(request.notes());
        lemma.setSource(Source.USER_ENTERED);
        lemma.setReviewStatus(ReviewStatus.PENDING);
        lemma.setProcessingStatus(ProcessingStatus.QUEUED);

        // Step 2: Save immediately (returns in < 1 second)
        Lemma saved = lemmaRepository.save(lemma);

        // Step 3: Trigger background processing (async, non-blocking)
        backgroundProcessingService.processLemma(saved.getId());

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
        List<Lemma> results = lemmaRepository.searchByText(query);
        return results.stream()
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
}
