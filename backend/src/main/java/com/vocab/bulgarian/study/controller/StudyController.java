package com.vocab.bulgarian.study.controller;

import com.vocab.bulgarian.study.domain.enums.ReviewRating;
import com.vocab.bulgarian.study.dto.*;
import com.vocab.bulgarian.study.service.StudySessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StudySessionService service;

    public StudyController(StudySessionService service) {
        this.service = service;
    }

    @PostMapping("/sessions")
    public ResponseEntity<StartSessionResponseDTO> startSession(
        @RequestParam(defaultValue = "100") int maxCards
    ) {
        return ResponseEntity.ok(service.startSession(maxCards));
    }

    @GetMapping("/sessions/{id}/next")
    public ResponseEntity<StudyCardDTO> getNextCard(@PathVariable Long id) {
        StudyCardDTO card = service.getNextCard(id);
        return card != null ? ResponseEntity.ok(card) : ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{id}/rate")
    public ResponseEntity<StudyCardDTO> rateCard(
        @PathVariable Long id,
        @RequestBody RateCardRequestDTO request
    ) {
        ReviewRating rating = ReviewRating.valueOf(request.rating().toUpperCase());
        StudyCardDTO next = service.rateCard(id, request.lemmaId(), rating);
        return next != null ? ResponseEntity.ok(next) : ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{id}/end")
    public ResponseEntity<SessionSummaryDTO> endSession(@PathVariable Long id) {
        return ResponseEntity.ok(service.endSession(id));
    }

    @GetMapping("/due-count")
    public ResponseEntity<DueCountDTO> getDueCount() {
        return ResponseEntity.ok(service.getDueCount());
    }
}
