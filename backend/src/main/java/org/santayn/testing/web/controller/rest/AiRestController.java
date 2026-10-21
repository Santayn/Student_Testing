package org.santayn.testing.web.controller.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.santayn.testing.models.ai.AiJob;
import org.santayn.testing.models.ai.AiJobStatus;
import org.santayn.testing.models.ai.AiJobType;
import org.santayn.testing.service.CurrentUserAccessService;
import org.santayn.testing.service.ai.AiFileStorage;
import org.santayn.testing.service.ai.AiImageClient;
import org.santayn.testing.service.ai.AiJobService;
import org.santayn.testing.service.ai.AiOcrService;
import org.santayn.testing.service.ai.AiProperties;
import org.santayn.testing.service.ai.ImageGenerationService;
import org.santayn.testing.service.ai.ImageSearchService;
import org.santayn.testing.service.ai.LectureScanService;
import org.santayn.testing.service.ai.PresentationBuildService;
import org.santayn.testing.service.ai.PresentationOutlineService;
import org.santayn.testing.service.ai.RemoteImageFetcher;
import org.santayn.testing.service.ai.TextAnswerGradingService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** ИИ для преподавателя: проверка ответов, сканы лекций, презентации. */
@RestController
@RequestMapping("/api/v1/ai")
public class AiRestController {

    private final AiProperties properties;
    private final AiJobService jobService;
    private final AiOcrService ocrService;
    private final AiImageClient imageClient;
    private final AiFileStorage fileStorage;
    private final TextAnswerGradingService gradingService;
    private final LectureScanService lectureScanService;
    private final PresentationOutlineService outlineService;
    private final PresentationBuildService buildService;
    private final ImageSearchService imageSearchService;
    private final ImageGenerationService imageGenerationService;
    private final RemoteImageFetcher imageFetcher;
    private final CurrentUserAccessService accessService;

    public AiRestController(AiProperties properties,
                            AiJobService jobService,
                            AiOcrService ocrService,
                            AiImageClient imageClient,
                            AiFileStorage fileStorage,
                            TextAnswerGradingService gradingService,
                            LectureScanService lectureScanService,
                            PresentationOutlineService outlineService,
                            PresentationBuildService buildService,
                            ImageSearchService imageSearchService,
                            ImageGenerationService imageGenerationService,
                            RemoteImageFetcher imageFetcher,
                            CurrentUserAccessService accessService) {
        this.properties = properties;
        this.jobService = jobService;
        this.ocrService = ocrService;
        this.imageClient = imageClient;
        this.fileStorage = fileStorage;
        this.gradingService = gradingService;
        this.lectureScanService = lectureScanService;
        this.outlineService = outlineService;
        this.buildService = buildService;
        this.imageSearchService = imageSearchService;
        this.imageGenerationService = imageGenerationService;
        this.imageFetcher = imageFetcher;
        this.accessService = accessService;
    }

    /** Что из ИИ доступно — клиенты показывают только работающие кнопки. Расход — в отчёте администратора. */
    @GetMapping("/status")
    public AiStatusResponse status() {
        boolean enabled = properties.textEnabled();
        return new AiStatusResponse(
                enabled,
                properties.normalizedProvider(),
                gradingService.isAiGradingAvailable(),
                lectureScanService.isAvailable(),
                ocrService.engine(),
                enabled,
                imageClient.isAvailable(),
                imageSearchService.isAvailable()
        );
    }

    @GetMapping("/jobs/{jobId}")
    public AiJobService.AiJobResponse job(@PathVariable Long jobId, Authentication authentication) {
        return jobService.toResponse(requireOwnJob(jobId, authentication));
    }

    /** Сгенерированная картинка (для превью в редакторе презентации). */
    @GetMapping("/jobs/{jobId}/image")
    public ResponseEntity<byte[]> generatedImage(@PathVariable Long jobId, Authentication authentication) throws IOException {
        AiJob job = requireOwnJob(jobId, authentication);
        JsonNode result = jobService.readResult(job);
        if (!AiJobType.IMAGE_GENERATION.equals(job.getType())
                || job.getStatus() != AiJobStatus.DONE
                || result == null
                || !result.hasNonNull("path")) {
            throw new IllegalArgumentException("Картинка ещё не готова.");
        }
        Path path = fileStorage.resolve(result.path("path").asText());
        byte[] content = Files.readAllBytes(path);
        return ResponseEntity.ok()
                .contentType(path.toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(content);
    }

    @PostMapping("/questions/criteria-suggestion")
    public CriteriaSuggestionResponse suggestCriteria(@Valid @RequestBody CriteriaSuggestionRequest request,
                                                      Authentication authentication) {
        return new CriteriaSuggestionResponse(gradingService.suggestCriteria(
                request.question(),
                request.correctAnswer(),
                request.points(),
                accessService.currentUser(authentication).personId()
        ));
    }

    @PostMapping(value = "/lecture-scans", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AiJobService.AiJobResponse scanLecture(@RequestParam("files") List<MultipartFile> files,
                                                  @RequestParam(required = false) Integer lectureId,
                                                  @RequestParam(defaultValue = "false") boolean attachOriginals,
                                                  Authentication authentication) {
        if (lectureId != null) {
            accessService.requireLectureOwner(authentication, lectureId);
        }
        return jobService.toResponse(lectureScanService.enqueue(
                accessService.currentUser(authentication).personId(),
                lectureId,
                files,
                attachOriginals
        ));
    }

    @PostMapping("/lectures/{lectureId}/presentation-outline")
    public AiJobService.AiJobResponse presentationOutline(@PathVariable Integer lectureId,
                                                          @Valid @RequestBody OutlineRequest request,
                                                          Authentication authentication) {
        accessService.requireLectureOwner(authentication, lectureId);
        return jobService.toResponse(outlineService.enqueue(
                accessService.currentUser(authentication).personId(),
                lectureId,
                request.slideCount() == null ? 10 : request.slideCount(),
                request.instructions()
        ));
    }

    @PostMapping("/lectures/{lectureId}/presentation")
    public AiJobService.AiJobResponse buildPresentation(@PathVariable Integer lectureId,
                                                        @Valid @RequestBody BuildRequest request,
                                                        Authentication authentication) {
        accessService.requireLectureOwner(authentication, lectureId);
        return jobService.toResponse(buildService.enqueue(
                accessService.currentUser(authentication).personId(),
                lectureId,
                request.outline(),
                request.withPdf() == null || request.withPdf()
        ));
    }

    @GetMapping("/images/search")
    public List<ImageSearchService.FoundImage> searchImages(@RequestParam("q") String query,
                                                            @RequestParam(defaultValue = "8") int limit) {
        return imageSearchService.search(query, limit);
    }

    /** Превью найденной картинки через сервер: браузер не ходит на внешние сайты (CSP img-src 'self'). */
    @GetMapping("/images/preview")
    public ResponseEntity<byte[]> previewImage(@RequestParam("url") String url) {
        RemoteImageFetcher.Image image = imageFetcher.fetch(url);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePrivate())
                .body(image.content());
    }

    @PostMapping("/images/generate")
    public AiJobService.AiJobResponse generateImage(@Valid @RequestBody GenerateImageRequest request,
                                                    Authentication authentication) {
        if (request.lectureId() != null) {
            accessService.requireLectureOwner(authentication, request.lectureId());
        }
        return jobService.toResponse(imageGenerationService.enqueue(
                accessService.currentUser(authentication).personId(),
                request.lectureId(),
                request.prompt()
        ));
    }

    private AiJob requireOwnJob(Long jobId, Authentication authentication) {
        AiJob job = jobService.get(jobId);
        if (accessService.isAdmin(authentication)) {
            return job;
        }
        Integer personId = accessService.currentUser(authentication).personId();
        if (job.getOwnerPersonId() == null || !Objects.equals(job.getOwnerPersonId(), personId)) {
            throw new AccessDeniedException("AI job belongs to another user.");
        }
        return job;
    }

    public record AiStatusResponse(boolean enabled,
                                   String provider,
                                   boolean grading,
                                   boolean lectureScan,
                                   String ocrEngine,
                                   boolean presentation,
                                   boolean imageGeneration,
                                   boolean imageSearch) {
    }

    public record CriteriaSuggestionRequest(@NotBlank @Size(max = 2000) String question,
                                            @Size(max = 4000) String correctAnswer,
                                            BigDecimal points) {
    }

    public record CriteriaSuggestionResponse(String criteria) {
    }

    public record OutlineRequest(Integer slideCount, @Size(max = 1000) String instructions) {
    }

    public record BuildRequest(@NotNull JsonNode outline, Boolean withPdf) {
    }

    public record GenerateImageRequest(@NotBlank @Size(max = 1000) String prompt, Integer lectureId) {
    }
}
