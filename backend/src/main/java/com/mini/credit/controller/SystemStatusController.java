package com.mini.credit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemStatusController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean databaseUp = isDatabaseUp();
        checks.put("database", databaseUp ? "UP" : "DOWN");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", databaseUp ? "UP" : "DOWN");
        response.put("timestamp", Instant.now());
        response.put("checks", checks);

        return ResponseEntity.status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean databaseUp = isDatabaseUp();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", "mini-credit-api");
        response.put("status", databaseUp ? "UP" : "DEGRADED");
        response.put("timestamp", Instant.now());
        response.put("database", databaseUp ? "UP" : "DOWN");
        return ResponseEntity.ok(response);
    }

    private boolean isDatabaseUp() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}