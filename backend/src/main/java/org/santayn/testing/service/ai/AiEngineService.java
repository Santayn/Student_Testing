package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.santayn.testing.models.ai.AiEngine;
import org.santayn.testing.models.ai.AiEngineSetting;
import org.santayn.testing.models.ai.AiFeature;
import org.santayn.testing.repository.AiEngineSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Какой движок выполняет функцию ИИ — внешний API или собственная модель. Выбор администратора
 * хранится в БД, без него действует значение из настроек (app.ai.engines.*).
 */
@Service
public class AiEngineService {

    private static final Duration CACHE = Duration.ofSeconds(10);

    private final AiProperties properties;
    private final AiEngineSettingRepository repository;
    private final LocalAiClient localAiClient;
    private final LocalLlmClient localLlmClient;

    private volatile Map<AiFeature, String> cached = Map.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public AiEngineService(AiProperties properties,
                           AiEngineSettingRepository repository,
                           LocalAiClient localAiClient,
                           LocalLlmClient localLlmClient) {
        this.properties = properties;
        this.repository = repository;
        this.localAiClient = localAiClient;
        this.localLlmClient = localLlmClient;
    }

    public String engine(AiFeature feature) {
        if (Instant.now().isAfter(cachedAt.plus(CACHE))) {
            reload();
        }
        String engine = cached.get(feature);
        return engine != null ? engine : defaultEngine(feature);
    }

    public boolean isLocal(AiFeature feature) {
        return AiEngine.LOCAL.equals(engine(feature));
    }

    /** Функция работает выбранным движком: API настроен или собственная модель подключена. */
    public boolean isAvailable(AiFeature feature) {
        return isLocal(feature) ? isLocalConfigured(feature) : isApiAvailable(feature);
    }

    public boolean isApiAvailable(AiFeature feature) {
        return switch (feature) {
            case GRADING -> properties.textEnabled() && properties.getGrading().isEnabled();
            case CRITERIA, LECTURE_SCAN -> properties.textEnabled();
        };
    }

    /** Подключение настроено (без проверки, загружена ли модель — это делает {@link #isLocalReady}). */
    public boolean isLocalConfigured(AiFeature feature) {
        return switch (feature) {
            case GRADING -> properties.getGrading().isEnabled() && localAiClient.isConfigured();
            case LECTURE_SCAN -> localAiClient.isConfigured();
            case CRITERIA -> localLlmClient.isConfigured();
        };
    }

    public boolean isLocalReady(AiFeature feature) {
        return switch (feature) {
            case GRADING -> localAiClient.isModelReady("grader");
            case LECTURE_SCAN -> localAiClient.isModelReady("layout");
            case CRITERIA -> localLlmClient.isModelReady();
        };
    }

    @Transactional
    public void setEngine(AiFeature feature, String engine, Integer personId) {
        String normalized = AiEngine.normalize(engine);
        if (AiEngine.LOCAL.equals(normalized) && !isLocalConfigured(feature)) {
            throw new IllegalArgumentException("Собственная модель для функции «" + feature.title() + "» не подключена.");
        }
        AiEngineSetting setting = repository.findById(feature.name()).orElseGet(AiEngineSetting::new);
        setting.setFeature(feature.name());
        setting.setEngine(normalized);
        setting.setUpdatedAtUtc(Instant.now());
        setting.setUpdatedByPersonId(personId);
        repository.save(setting);
        cachedAt = Instant.EPOCH;
    }

    /** Состояние функций для страницы настроек администратора. */
    @Transactional(readOnly = true)
    public List<FeatureStatus> overview() {
        reload();
        JsonNode health = localAiClient.health();
        return Arrays.stream(AiFeature.values()).map(feature -> {
            String localModel = null;
            JsonNode metrics = null;
            if (health != null && feature != AiFeature.CRITERIA) {
                JsonNode model = health.path(feature == AiFeature.GRADING ? "grader" : "layout");
                localModel = AiText.trimToNull(model.path("name").asText(null));
                metrics = model.path("metrics").isObject() ? model.path("metrics") : null;
            } else if (feature == AiFeature.CRITERIA && localLlmClient.isConfigured()) {
                localModel = properties.getLocal().getCriteriaModel();
            }
            return new FeatureStatus(
                    feature.key(),
                    feature.title(),
                    engine(feature),
                    defaultEngine(feature),
                    isApiAvailable(feature),
                    isLocalConfigured(feature),
                    isLocalConfigured(feature) && isLocalReady(feature),
                    localModel,
                    metrics
            );
        }).toList();
    }

    private void reload() {
        Map<AiFeature, String> values = new EnumMap<>(AiFeature.class);
        for (AiEngineSetting setting : repository.findAll()) {
            try {
                values.put(AiFeature.valueOf(setting.getFeature()), AiEngine.normalize(setting.getEngine()));
            } catch (IllegalArgumentException ignored) {
                // запись от старой версии — пропускаем
            }
        }
        cached = values;
        cachedAt = Instant.now();
    }

    private String defaultEngine(AiFeature feature) {
        String value = switch (feature) {
            case GRADING -> properties.getEngines().getGrading();
            case CRITERIA -> properties.getEngines().getCriteria();
            case LECTURE_SCAN -> properties.getEngines().getLectureScan();
        };
        try {
            return AiEngine.normalize(value);
        } catch (IllegalArgumentException error) {
            return AiEngine.API;
        }
    }

    public record FeatureStatus(String feature,
                                String title,
                                String engine,
                                String defaultEngine,
                                boolean apiAvailable,
                                boolean localConfigured,
                                boolean localReady,
                                String localModel,
                                JsonNode localMetrics) {
    }
}
