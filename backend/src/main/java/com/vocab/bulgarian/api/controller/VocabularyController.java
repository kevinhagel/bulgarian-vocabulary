package com.vocab.bulgarian.api.controller;

import com.vocab.bulgarian.api.dto.*;
import com.vocab.bulgarian.domain.enums.DifficultyLevel;
import com.vocab.bulgarian.domain.enums.PartOfSpeech;
import com.vocab.bulgarian.domain.enums.ReviewStatus;
import com.vocab.bulgarian.domain.enums.Source;
import com.vocab.bulgarian.service.VocabularyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for vocabulary management operations.
 * Provides CRUD endpoints, search, filtering, and review workflow.
 */
@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    /**
     * Create new vocabulary entry with LLM processing.
     * POST /api/vocabulary
     *
     * @param request create request with word form, translation, and notes
     * @return 201 Created with full detail DTO including LLM-generated metadata
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<LemmaDetailDTO>> createVocabulary(
        @Validated(OnCreate.class) @RequestBody CreateLemmaRequestDTO request
    ) {
        return vocabularyService.createVocabulary(request)
            .thenApply(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    /**
     * Get vocabulary entry detail by ID.
     * GET /api/vocabulary/{id}
     *
     * @param id lemma ID
     * @return 200 OK with detail DTO including full inflections list
     */
    @GetMapping("/{id}")
    public ResponseEntity<LemmaDetailDTO> getVocabularyById(@PathVariable Long id) {
        LemmaDetailDTO dto = vocabularyService.getVocabularyById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Browse/filter vocabulary with pagination.
     * GET /api/vocabulary
     *
     * @param source optional source filter
     * @param partOfSpeech optional part of speech filter
     * @param difficultyLevel optional difficulty level filter
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @param sort sort field (default "text")
     * @return 200 OK with paginated results
     */
    @GetMapping
    public ResponseEntity<Page<LemmaResponseDTO>> browseVocabulary(
        @RequestParam(required = false) Source source,
        @RequestParam(required = false) PartOfSpeech partOfSpeech,
        @RequestParam(required = false) DifficultyLevel difficultyLevel,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "text") String sort
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<LemmaResponseDTO> results = vocabularyService.browseVocabulary(
            source, partOfSpeech, difficultyLevel, pageable
        );
        return ResponseEntity.ok(results);
    }

    /**
     * Full-text search vocabulary using PGroonga (Cyrillic support).
     * GET /api/vocabulary/search
     *
     * @param q search query
     * @return 200 OK with search results (up to 20)
     */
    @GetMapping("/search")
    public ResponseEntity<List<LemmaResponseDTO>> searchVocabulary(@RequestParam String q) {
        List<LemmaResponseDTO> results = vocabularyService.searchVocabulary(q);
        return ResponseEntity.ok(results);
    }

    /**
     * Update vocabulary entry.
     * PUT /api/vocabulary/{id}
     *
     * @param id lemma ID
     * @param request update request
     * @return 200 OK with updated detail DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<LemmaDetailDTO> updateVocabulary(
        @PathVariable Long id,
        @Validated(OnUpdate.class) @RequestBody UpdateLemmaRequestDTO request
    ) {
        LemmaDetailDTO dto = vocabularyService.updateVocabulary(id, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * Delete vocabulary entry.
     * DELETE /api/vocabulary/{id}
     *
     * @param id lemma ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVocabulary(@PathVariable Long id) {
        vocabularyService.deleteVocabulary(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update review status for vocabulary entry.
     * PATCH /api/vocabulary/{id}/review-status
     *
     * @param id lemma ID
     * @param status new review status
     * @return 200 OK with updated detail DTO
     */
    @PatchMapping("/{id}/review-status")
    public ResponseEntity<LemmaDetailDTO> updateReviewStatus(
        @PathVariable Long id,
        @RequestParam ReviewStatus status
    ) {
        LemmaDetailDTO dto = vocabularyService.updateReviewStatus(id, status);
        return ResponseEntity.ok(dto);
    }

    /**
     * Reprocess a vocabulary entry through the LLM pipeline.
     * POST /api/vocabulary/{id}/reprocess
     *
     * @param id lemma ID
     * @param request optional disambiguation hint
     * @return 200 OK with updated detail DTO (status: QUEUED)
     */
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<LemmaDetailDTO> reprocessVocabulary(
        @PathVariable Long id,
        @RequestBody(required = false) ReprocessRequestDTO request
    ) {
        LemmaDetailDTO dto = vocabularyService.reprocessVocabulary(id, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * Flag a vocabulary entry as needing correction.
     * POST /api/vocabulary/{id}/flag
     *
     * @param id lemma ID
     * @return 200 OK with updated detail DTO
     */
    @PostMapping("/{id}/flag")
    public ResponseEntity<LemmaDetailDTO> flagVocabulary(@PathVariable Long id) {
        LemmaDetailDTO dto = vocabularyService.flagVocabulary(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get the review queue: user-entered words that are PENDING or NEEDS_CORRECTION.
     * GET /api/vocabulary/review-queue
     *
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @return 200 OK with paginated review queue
     */
    @GetMapping("/review-queue")
    public ResponseEntity<Page<LemmaResponseDTO>> getReviewQueue(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<LemmaResponseDTO> results = vocabularyService.getReviewQueue(PageRequest.of(page, size));
        return ResponseEntity.ok(results);
    }
}
