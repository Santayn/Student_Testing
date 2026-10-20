package org.santayn.testing.web.controller.rest;

import org.santayn.testing.service.ai.AiUsageReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Расход ИИ — только администраторам и пользователям с правом ai.usage.read (см. SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/ai-usage")
public class AdminAiUsageRestController {

    private final AiUsageReportService reportService;

    public AdminAiUsageRestController(AiUsageReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public AiUsageReportService.Report report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.report(from, to);
    }
}
