package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.santayn.testing.models.ai.AiJob;
import org.santayn.testing.models.ai.AiJobStatus;
import org.santayn.testing.models.ai.AiJobType;
import org.santayn.testing.models.lecture.LectureMaterial;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.service.LectureMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Сборка презентации по утверждённому плану: картинки (найденные, сгенерированные или из
 * материалов лекции) скачиваются, PPTX и PDF сохраняются в материалы лекции — их сразу видят
 * студенты и мобильное приложение (в том числе без сети).
 */
@Service
public class PresentationBuildService implements AiJobHandler {

    private static final String PPTX_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private final AiJobService jobService;
    private final AiFileStorage fileStorage;
    private final RemoteImageFetcher imageFetcher;
    private final LectureRepository lectureRepository;
    private final LectureMaterialService lectureMaterialService;

    public PresentationBuildService(AiJobService jobService,
                                    AiFileStorage fileStorage,
                                    RemoteImageFetcher imageFetcher,
                                    LectureRepository lectureRepository,
                                    LectureMaterialService lectureMaterialService) {
        this.jobService = jobService;
        this.fileStorage = fileStorage;
        this.imageFetcher = imageFetcher;
        this.lectureRepository = lectureRepository;
        this.lectureMaterialService = lectureMaterialService;
    }

    @Transactional
    public AiJob enqueue(Integer ownerPersonId, Integer lectureId, JsonNode outline, boolean withPdf) {
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

        Map<Integer, PptxPresentationBuilder.SlideImage> images = new HashMap<>();
        int index = 0;
        for (JsonNode slide : outline.path("slides")) {
            JsonNode selected = slide.path("image").path("selected");
            if (selected.isObject()) {
                context.progress("Загружаю картинку для слайда " + (index + 1));
                PptxPresentationBuilder.SlideImage image = loadImage(selected, lectureId, job.getOwnerPersonId());
                if (image != null) {
                    images.put(index, image);
                }
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
