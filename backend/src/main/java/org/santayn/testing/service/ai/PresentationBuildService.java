package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.santayn.testing.models.ai.AiJob;
import org.santayn.testing.models.ai.AiJobStatus;
import org.santayn.testing.models.ai.AiJobType;
import org.santayn.testing.models.lecture.LectureMaterial;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.service.LectureMaterialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Сборка презентации по утверждённому плану: картинки (найденные, сгенерированные или из
 * материалов лекции) скачиваются, PPTX и PDF сохраняются в материалы лекции — их сразу видят
 * студенты и мобильное приложение (в том числе без сети). Если на слайде предложен поиск картинки,
 * а преподаватель её не выбрал, берётся первая подходящая находка в банке изображений.
 */
@Service
public class PresentationBuildService implements AiJobHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PresentationBuildService.class);
    private static final String PPTX_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private final AiJobService jobService;
    private final AiFileStorage fileStorage;
    private final RemoteImageFetcher imageFetcher;
    private final ImageSearchService imageSearchService;
    private final LectureRepository lectureRepository;
    private final LectureMaterialService lectureMaterialService;

    public PresentationBuildService(AiJobService jobService,
                                    AiFileStorage fileStorage,
                                    RemoteImageFetcher imageFetcher,
                                    ImageSearchService imageSearchService,
                                    LectureRepository lectureRepository,
                                    LectureMaterialService lectureMaterialService) {
        this.jobService = jobService;
        this.fileStorage = fileStorage;
        this.imageFetcher = imageFetcher;
        this.imageSearchService = imageSearchService;
        this.lectureRepository = lectureRepository;
        this.lectureMaterialService = lectureMaterialService;
    }

    @Transactional
    public AiJob enqueue(Integer ownerPersonId, Integer lectureId, JsonNode outline, boolean withPdf) {
        return enqueue(ownerPersonId, lectureId, outline, withPdf, true);
    }

    /** @param autoImages подобрать картинки слайдам, где предложен поиск, но картинка не выбрана */
    @Transactional
    public AiJob enqueue(Integer ownerPersonId, Integer lectureId, JsonNode outline, boolean withPdf,
                         boolean autoImages) {
        if (!lectureRepository.existsById(lectureId)) {
            throw new IllegalArgumentException("Lecture not found: " + lectureId);
        }
        if (outline == null || !outline.path("slides").isArray() || outline.path("slides").isEmpty()) {
            throw new IllegalArgumentException("В плане презентации нет слайдов.");
        }
        if (outline.path("slides").size() > 30) {
            throw new IllegalArgumentException("В презентации может быть до 30 слайдов.");
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("outline", outline);
        input.put("withPdf", withPdf);
        input.put("autoImages", autoImages);
        return jobService.enqueue(AiJobType.PRESENTATION_BUILD, ownerPersonId, lectureId.longValue(), input);
    }

    @Override
    public String type() {
        return AiJobType.PRESENTATION_BUILD;
    }

    @Override
    public int maxAttempts() {
        return 1;
    }

    @Override
    public Object handle(AiJob job, AiJobContext context) throws Exception {
        JsonNode input = jobService.readInput(job);
        JsonNode outline = input.path("outline");
        Integer lectureId = job.getTargetId().intValue();

        boolean autoImages = input.path("autoImages").asBoolean(true) && imageSearchService.isAvailable();
        Map<Integer, PptxPresentationBuilder.SlideImage> images = new HashMap<>();
        int index = 0;
        for (JsonNode slide : outline.path("slides")) {
            JsonNode image = slide.path("image");
            JsonNode selected = image.path("selected");
            PptxPresentationBuilder.SlideImage loaded = null;
            if (selected.isObject()) {
                context.progress("Загружаю картинку для слайда " + (index + 1));
                loaded = loadImage(selected, lectureId, job.getOwnerPersonId());
            } else if (autoImages && "search".equals(image.path("mode").asText())
                    && !image.path("query").asText("").isBlank()) {
                context.progress("Подбираю картинку для слайда " + (index + 1));
                loaded = findImage(image.path("query").asText());
            }
            if (loaded != null) {
                images.put(index, loaded);
            }
            index++;
        }

        context.progress("Собираю презентацию");
        boolean withPdf = input.path("withPdf").asBoolean(true);
        PptxPresentationBuilder.Result built = PptxPresentationBuilder.build(outline, images, withPdf);
        String baseName = "Презентация — " + safeFileName(outline.path("title").asText("лекция"));
        LectureMaterial pptx = lectureMaterialService.saveGenerated(lectureId, baseName + ".pptx", PPTX_TYPE, built.pptx());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("materialId", pptx.getId());
        result.put("fileName", pptx.getFileName());
        result.put("slides", built.slideCount());
        if (built.pdf() != null) {
            LectureMaterial pdf = lectureMaterialService.saveGenerated(lectureId, baseName + ".pdf", "application/pdf", built.pdf());
            result.put("pdfMaterialId", pdf.getId());
            result.put("pdfFileName", pdf.getFileName());
        }
        return result;
    }

    /** Первая картинка из банка, которую удалось скачать; при любой неудаче слайд остаётся без картинки. */
    private PptxPresentationBuilder.SlideImage findImage(String query) {
        List<ImageSearchService.FoundImage> found;
        try {
            found = imageSearchService.search(query, 12).images();
        } catch (RuntimeException searchFailed) {
            LOGGER.info("Картинка для запроса «{}» не найдена: {}", query, searchFailed.getMessage());
            return null;
        }
        for (ImageSearchService.FoundImage candidate : found) {
            if (!ImageSearchService.matchesQuery(candidate, query)) {
                continue; // на абстрактные запросы банки находят случайные фото — такие не берём
            }
            try {
                RemoteImageFetcher.Image image = imageFetcher.fetch(candidate.imageUrl());
                return new PptxPresentationBuilder.SlideImage(image.content(), candidate.attribution());
            } catch (RuntimeException fetchFailed) {
                LOGGER.info("Не удалось скачать {}: {}", candidate.imageUrl(), fetchFailed.getMessage());
            }
        }
        return null;
    }

    private PptxPresentationBuilder.SlideImage loadImage(JsonNode selected, Integer lectureId, Integer ownerPersonId) throws Exception {
        String kind = selected.path("kind").asText("");
        switch (kind) {
            case "search" -> {
                RemoteImageFetcher.Image image = imageFetcher.fetch(selected.path("url").asText(""));
                return new PptxPresentationBuilder.SlideImage(image.content(), attribution(selected));
            }
            case "generated" -> {
                AiJob imageJob = jobService.get(selected.path("jobId").asLong());
                if (!AiJobType.IMAGE_GENERATION.equals(imageJob.getType())
                        || imageJob.getStatus() != AiJobStatus.DONE
                        || !Objects.equals(imageJob.getOwnerPersonId(), ownerPersonId)) {
                    return null;
                }
                JsonNode result = jobService.readResult(imageJob);
                if (result == null || !result.hasNonNull("path")) {
                    return null;
                }
                return new PptxPresentationBuilder.SlideImage(
                        Files.readAllBytes(fileStorage.resolve(result.path("path").asText())),
                        "сгенерировано ИИ"
                );
            }
            case "material" -> {
                LectureMaterialService.StoredLectureMaterial stored =
                        lectureMaterialService.load(lectureId, selected.path("materialId").asInt());
                String type = stored.material().getContentType() == null ? "" : stored.material().getContentType();
                String name = stored.material().getFileName().toLowerCase(java.util.Locale.ROOT);
                boolean image = type.startsWith("image/") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
                return image
                        ? new PptxPresentationBuilder.SlideImage(Files.readAllBytes(stored.path()), null)
                        : null;
            }
            default -> {
                return null;
            }
        }
    }

    static String attribution(JsonNode selected) {
        return new ImageSearchService.FoundImage(
                selected.path("source").asText(""),
                selected.path("title").asText(""),
                selected.path("url").asText(""),
                selected.path("author").asText(""),
                selected.path("license").asText(""),
                selected.path("licenseUrl").asText(""),
                selected.path("pageUrl").asText("")
        ).attribution();
    }

    static String safeFileName(String value) {
        String cleaned = value.replaceAll("[\\\\/:*?\"<>|]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            return "лекция";
        }
        return cleaned.length() > 80 ? cleaned.substring(0, 80).trim() : cleaned;
    }
}
