package com.vocab.bulgarian.study.controller;

import com.vocab.bulgarian.study.dto.LemmaStatsDTO;
import com.vocab.bulgarian.study.dto.ProgressDashboardDTO;
import com.vocab.bulgarian.study.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/progress")
    public ResponseEntity<ProgressDashboardDTO> getDashboard() {
        return ResponseEntity.ok(progressService.getDashboard());
    }

    @GetMapping("/stats/{lemmaId}")
    public ResponseEntity<LemmaStatsDTO> getLemmaStats(@PathVariable Long lemmaId) {
        return ResponseEntity.ok(progressService.getLemmaStats(lemmaId));
    }
}
