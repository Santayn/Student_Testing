package org.santayn.testing.web.controller.rest;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MainRestController {

    private final JdbcTemplate jdbcTemplate;

    public MainRestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "ok",
                "backend", "spring-adapted-test-platform",
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/status/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of(
                    "status", "ready",
                    "database", "up",
                    "timestamp", Instant.now().toString()
            ));
        } catch (DataAccessException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "not_ready",
                    "database", "down",
                    "timestamp", Instant.now().toString()
            ));
        }
    }
}
