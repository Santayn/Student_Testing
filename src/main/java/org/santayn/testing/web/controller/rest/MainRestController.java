package org.santayn.testing.web.controller.rest;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping({"/api", "/api/v1"})
public class MainRestController {

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "ok",
                "backend", "spring-adapted-test-platform",
                "timestamp", Instant.now().toString()
        );
    }
}
