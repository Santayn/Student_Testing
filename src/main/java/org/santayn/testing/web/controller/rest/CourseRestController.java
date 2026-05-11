package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.CourseService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping({"/api/courses", "/api/v1/courses"})
public class CourseRestController {

    private final CourseService courseService;

    public CourseRestController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/templates")
    public List<ApiResponses.CourseTemplateResponse> templates(@RequestParam(required = false) Integer subjectId,
                                                               @RequestParam(required = false) Integer authorPersonId,
                                                               @RequestParam(defaultValue = "false") boolean publicOnly) {
        return ApiResponses.list(courseService.findTemplates(subjectId, authorPersonId, publicOnly), ApiResponses::courseTemplate);
    }

    @GetMapping("/templates/{templateId}")
    public ApiResponses.CourseTemplateResponse template(@PathVariable Integer templateId) {
        return ApiResponses.courseTemplate(courseService.getTemplate(templateId));
    }

    @PostMapping("/templates")
    public ApiResponses.CourseTemplateResponse createTemplate(@Valid @RequestBody CourseTemplateRequest request) {
        return ApiResponses.courseTemplate(courseService.createTemplate(
                request.subjectId(),
                request.authorPersonId(),
                request.name(),
                request.publicVisible()
        ));
    }

    @PutMapping("/templates/{templateId}")
    public ApiResponses.CourseTemplateResponse updateTemplate(@PathVariable Integer templateId,
                                                              @Valid @RequestBody CourseTemplateRequest request) {
        return ApiResponses.courseTemplate(courseService.updateTemplate(
                templateId,
                request.subjectId(),
                request.authorPersonId(),
                request.name(),
                request.publicVisible()
        ));
    }

    @DeleteMapping("/templates/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Integer templateId) {
        courseService.deleteTemplate(templateId);
    }

    @PutMapping("/versions/{versionId}/publish")
    public ApiResponses.CourseVersionResponse publishVersion(@PathVariable Integer versionId,
                                                             @Valid @RequestBody PublishVersionRequest request) {
        return ApiResponses.courseVersion(courseService.publishVersion(versionId, request.publishedByPersonId()));
    }

    @PutMapping("/versions/{versionId}/unpublish")
    public ApiResponses.CourseVersionResponse unpublishVersion(@PathVariable Integer versionId) {
        return ApiResponses.courseVersion(courseService.unpublishVersion(versionId));
    }

    @GetMapping("/templates/{templateId}/versions")
    public List<ApiResponses.CourseVersionResponse> versions(@PathVariable Integer templateId) {
        return ApiResponses.list(courseService.findVersions(templateId), ApiResponses::courseVersion);
    }

    @GetMapping("/versions/{versionId}")
    public ApiResponses.CourseVersionResponse version(@PathVariable Integer versionId) {
        return ApiResponses.courseVersion(courseService.getVersion(versionId));
    }

    @PostMapping("/templates/{templateId}/versions")
    public ApiResponses.CourseVersionResponse createVersion(@PathVariable Integer templateId,
                                                            @Valid @RequestBody CourseVersionRequest request) {
        return ApiResponses.courseVersion(courseService.createVersion(
                templateId,
                request.versionNumber(),
                request.title(),
                request.description(),
                request.createdByPersonId(),
                request.published(),
                request.publishedByPersonId(),
                request.changeNotes()
        ));
    }

    @PutMapping("/versions/{versionId}")
    public ApiResponses.CourseVersionResponse updateVersion(@PathVariable Integer versionId,
                                                            @Valid @RequestBody CourseVersionUpdateRequest request) {
        return ApiResponses.courseVersion(courseService.updateVersion(
                versionId,
                request.versionNumber(),
                request.title(),
                request.description(),
                request.createdByPersonId(),
                request.changeNotes()
        ));
    }

    public record CourseTemplateRequest(
            @NotNull Integer subjectId,
            @NotNull Integer authorPersonId,
            @NotBlank @Size(max = 200) String name,
            boolean publicVisible
    ) {
    }

    public record CourseVersionRequest(
            @Positive int versionNumber,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull Integer createdByPersonId,
            boolean published,
            Integer publishedByPersonId,
            @Size(max = 2000) String changeNotes
    ) {
    }

    public record CourseVersionUpdateRequest(
            @Positive int versionNumber,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull Integer createdByPersonId,
            @Size(max = 2000) String changeNotes
    ) {
    }

    public record PublishVersionRequest(
            @NotNull Integer publishedByPersonId
    ) {
    }
    }
