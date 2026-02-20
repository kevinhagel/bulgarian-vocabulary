package com.vocab.bulgarian.api.controller;

import com.vocab.bulgarian.api.dto.LemmaDetailDTO;
import com.vocab.bulgarian.service.SentenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for example sentence generation.
 */
@RestController
@RequestMapping("/api/vocabulary")
public class SentenceController {

    private final SentenceService sentenceService;

    public SentenceController(SentenceService sentenceService) {
        this.sentenceService = sentenceService;
    }

    /**
     * Trigger on-demand sentence generation for a vocabulary entry.
     * POST /api/vocabulary/{id}/sentences/generate
     * Returns 202 Accepted with current DTO (sentenceStatus=QUEUED).
     * Generation happens in background; poll GET /api/vocabulary/{id} for completion.
     */
    @PostMapping("/{id}/sentences/generate")
    public ResponseEntity<LemmaDetailDTO> generateSentences(@PathVariable Long id) {
        LemmaDetailDTO dto = sentenceService.generateSentences(id);
        return ResponseEntity.accepted().body(dto);
    }

    /**
     * Batch backfill: generate sentences for all words that don't have them yet.
     * POST /api/vocabulary/sentences/generate-all
     * Returns count of lemmas queued.
     */
    @PostMapping("/sentences/generate-all")
    public ResponseEntity<Map<String, Integer>> generateAll() {
        int queued = sentenceService.queueBatchGeneration();
        return ResponseEntity.accepted().body(Map.of("queued", queued));
    }
}
