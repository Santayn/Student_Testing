package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.santayn.testing.models.ai.AiFeature;
import org.santayn.testing.service.CurrentUserAccessService;
import org.santayn.testing.service.ai.AiEngineService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Выбор движка функций ИИ: внешний API или собственная модель.
 * Только администраторам и пользователям с правом ai.settings.manage (см. SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin/ai-settings")
public class AdminAiSettingsRestController {

    private final AiEngineService engineService;
    private final CurrentUserAccessService accessService;

    public AdminAiSettingsRestController(AiEngineService engineService, CurrentUserAccessService accessService) {
        this.engineService = engineService;
        this.accessService = accessService;
    }

    @GetMapping
    public List<AiEngineService.FeatureStatus> features() {
        return engineService.overview();
    }

    @PutMapping("/{feature}")
    public List<AiEngineService.FeatureStatus> setEngine(@PathVariable String feature,
                                                         @Valid @RequestBody EngineRequest request,
                                                         Authentication authentication) {
        engineService.setEngine(
                AiFeature.fromKey(feature),
                request.engine(),
                accessService.currentUser(authentication).personId()
        );
        return engineService.overview();
    }

    public record EngineRequest(@NotBlank String engine) {
    }
}
