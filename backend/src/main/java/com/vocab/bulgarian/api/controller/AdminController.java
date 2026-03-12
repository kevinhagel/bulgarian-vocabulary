package com.vocab.bulgarian.api.controller;

import com.vocab.bulgarian.api.dto.AdminStatsDTO;
import com.vocab.bulgarian.dictionary.service.KaikkiImportService;
import com.vocab.bulgarian.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final KaikkiImportService kaikkiImportService;

    public AdminController(AdminService adminService, KaikkiImportService kaikkiImportService) {
        this.adminService = adminService;
        this.kaikkiImportService = kaikkiImportService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<Void> clearCache() {
        adminService.clearCache();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dictionary/import")
    public ResponseEntity<Map<String, Object>> importDictionary() {
        try {
            Path jsonlPath = Path.of("data/kaikki-bulgarian.jsonl");
            if (!jsonlPath.toFile().exists()) {
                // Try absolute path for Docker
                jsonlPath = Path.of("/app/data/kaikki-bulgarian.jsonl");
            }
            log.info("Starting dictionary import from {}", jsonlPath);
            var result = kaikkiImportService.importFromJsonl(jsonlPath);
            return ResponseEntity.ok(Map.of(
                "words", result.wordCount(),
                "forms", result.formCount(),
                "skipped", result.skipped(),
                "errors", result.errors()
            ));
        } catch (Exception e) {
            log.error("Dictionary import failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
