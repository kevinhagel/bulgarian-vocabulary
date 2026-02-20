package com.vocab.bulgarian.api.controller;

import com.vocab.bulgarian.api.dto.AdminStatsDTO;
import com.vocab.bulgarian.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
}
