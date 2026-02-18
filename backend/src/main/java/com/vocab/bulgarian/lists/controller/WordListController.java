package com.vocab.bulgarian.lists.controller;

import com.vocab.bulgarian.lists.dto.*;
import com.vocab.bulgarian.lists.service.WordListService;
import com.vocab.bulgarian.study.dto.StartSessionResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class WordListController {

    private final WordListService service;

    public WordListController(WordListService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<WordListSummaryDTO>> getAllLists() {
        return ResponseEntity.ok(service.getAllLists());
    }

    @PostMapping
    public ResponseEntity<WordListSummaryDTO> createList(
            @Valid @RequestBody CreateWordListRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createList(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WordListDetailDTO> getListDetail(@PathVariable Long id) {
        return ResponseEntity.ok(service.getListDetail(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WordListSummaryDTO> renameList(
            @PathVariable Long id,
            @Valid @RequestBody RenameWordListRequestDTO request) {
        return ResponseEntity.ok(service.renameList(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteList(@PathVariable Long id) {
        service.deleteList(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addLemma(
            @PathVariable Long id,
            @Valid @RequestBody AddLemmaRequestDTO request) {
        service.addLemma(id, request.lemmaId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{lemmaId}")
    public ResponseEntity<Void> removeLemma(
            @PathVariable Long id,
            @PathVariable Long lemmaId) {
        service.removeLemma(id, lemmaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sessions")
    public ResponseEntity<StartSessionResponseDTO> startListSession(
            @PathVariable Long id,
            @RequestParam(defaultValue = "DUE") String mode,
            @RequestParam(defaultValue = "20") int maxCards) {
        return ResponseEntity.ok(service.startListSession(id, mode, maxCards));
    }
}
