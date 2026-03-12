package com.vocab.bulgarian.dictionary.controller;

import com.vocab.bulgarian.dictionary.dto.DictionarySearchResultDTO;
import com.vocab.bulgarian.dictionary.service.DictionaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<DictionarySearchResultDTO>> search(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<DictionarySearchResultDTO> results = dictionaryService.searchByForm(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DictionarySearchResultDTO> getById(@PathVariable Long id) {
        return dictionaryService.getById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
