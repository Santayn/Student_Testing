package org.santayn.testing.service.ai;

import org.santayn.testing.models.ai.AiJob;
import org.santayn.testing.models.ai.AiJobType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Генерация иллюстрации для слайда. Картинка сохраняется в хранилище ИИ, ссылка — в результате задачи. */
@Service
public class ImageGenerationService implements AiJobHandler {

    private final AiImageClient imageClient;
    private final AiJobService jobService;
    private final AiFileStorage fileStorage;
    private final AiUsageService usageService;

    public ImageGenerationService(AiImageClient imageClient,
                                  AiJobService jobService,
                                  AiFileStorage fileStorage,
                                  AiUsageService usageService) {
        this.imageClient = imageClient;
        this.jobService = jobService;
        this.fileStorage = fileStorage;
        this.usageService = usageService;
    }

    @Transactional
    public AiJob enqueue(Integer ownerPersonId, Integer lectureId, String prompt) {
        if (!imageClient.isAvailable()) {
            throw new AiUnavailableException("Генерация картинок недоступна для выбранного провайдера ИИ.");
        }
        usageService.requireTokenBudget();
        String normalized = AiText.trimToNull(prompt);
        if (normalized == null) {
            throw new IllegalArgumentException("Опишите, что должно быть на картинке.");
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prompt", AiText.truncate(normalized, AiImageClient.MAX_PROMPT_LENGTH));
        return jobService.enqueue(
                AiJobType.IMAGE_GENERATION,
                ownerPersonId,
                lectureId == null ? null : lectureId.longValue(),
                input
        );
    }

    @Override
    public String type() {
        return AiJobType.IMAGE_GENERATION;
    }

    @Override
    public Object handle(AiJob job, AiJobContext context) {
        String prompt = jobService.readInput(job).path("prompt").asText("");
        byte[] image = imageClient.generate(prompt);
        context.addGeneratedImages(1);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", fileStorage.saveImage(image, isPng(image) ? "png" : "jpg"));
        result.put("prompt", prompt);
        return result;
    }

    static boolean isPng(byte[] content) {
        return content.length > 4
                && (content[0] & 0xFF) == 0x89
                && content[1] == 'P'
                && content[2] == 'N'
                && content[3] == 'G';
    }
}
