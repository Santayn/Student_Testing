package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.CourseService;
import org.santayn.testing.service.CurrentUserAccessService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseRestController {

    private final CourseService courseService;
    private final CurrentUserAccessService accessService;

    public CourseRestController(CourseService courseService, CurrentUserAccessService accessService) {
        this.courseService = courseService;
        this.accessService = accessService;
    }

    @GetMapping("/templates")
    public List<ApiResponses.CourseTemplateResponse> templates(@RequestParam(required = false) Integer subjectId,
                                                               @RequestParam(required = false) Integer authorPersonId,
                                                               @RequestParam(defaultValue = "false") boolean publicOnly,
                                                               Authentication authentication) {
        Integer effectiveAuthorId = accessService.isAdmin(authentication)
                ? authorPersonId
                : accessService.currentPersonId(authentication);
        if (!accessService.isAdmin(authentication) && subjectId != null) {
            accessService.requireSubjectOwner(authentication, subjectId);
        }
        return ApiResponses.list(courseService.findTemplates(subjectId, effectiveAuthorId, publicOnly), ApiResponses::courseTemplate);
    }

    @GetMapping("/templates/{templateId}")
    public ApiResponses.CourseTemplateResponse template(@PathVariable Integer templateId, Authentication authentication) {
        accessService.requireCourseTemplateOwner(authentication, templateId);
        return ApiResponses.courseTemplate(courseService.getTemplate(templateId));
    }

    @PostMapping("/templates")
    public ApiResponses.CourseTemplateResponse createTemplate(@Valid @RequestBody CourseTemplateRequest request,
                                                              Authentication authentication) {
        accessService.requireSubjectOwner(authentication, request.subjectId());
        return ApiResponses.courseTemplate(courseService.createTemplate(
                request.subjectId(),
                accessService.currentPersonId(authentication),
                request.name(),
                request.publicVisible()
        ));
    }

    @PutMapping("/templates/{templateId}")
    public ApiResponses.CourseTemplateResponse updateTemplate(@PathVariable Integer templateId,
                                                              @Valid @RequestBody CourseTemplateRequest request,
                                                              Authentication authentication) {
        accessService.requireCourseTemplateOwner(authentication, templateId);
        accessService.requireSubjectOwner(authentication, request.subjectId());
        Integer authorPersonId = accessService.isAdmin(authentication)
                ? courseService.getTemplate(templateId).getAuthorPersonId()
                : accessService.currentPersonId(authentication);
        return ApiResponses.courseTemplate(courseService.updateTemplate(
                templateId,
                request.subjectId(),
                authorPersonId,
                request.name(),
                request.publicVisible()
        ));
    }

    @DeleteMapping("/templates/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Integer templateId, Authentication authentication) {
        accessService.requireCourseTemplateOwner(authentication, templateId);
        courseService.deleteTemplate(templateId);
    }

    @PutMapping("/versions/{versionId}/publish")
    public ApiResponses.CourseVersionResponse publishVersion(@PathVariable Integer versionId,
                                                             @Valid @RequestBody PublishVersionRequest request,
                                                             Authentication authentication) {
        accessService.requireCourseVersionOwner(authentication, versionId);
        return ApiResponses.courseVersion(courseService.publishVersion(
                versionId, accessService.currentPersonId(authentication)
        ));
    }

    @PutMapping("/versions/{versionId}/unpublish")
    public ApiResponses.CourseVersionResponse unpublishVersion(@PathVariable Integer versionId,
                                                               Authentication authentication) {
        accessService.requireCourseVersionOwner(authentication, versionId);
        return ApiResponses.courseVersion(courseService.unpublishVersion(versionId));
    }

    @GetMapping("/templates/{templateId}/versions")
    public List<ApiResponses.CourseVersionResponse> versions(@PathVariable Integer templateId,
                                                             Authentication authentication) {
        accessService.requireCourseTemplateOwner(authentication, templateId);
        return ApiResponses.list(courseService.findVersions(templateId), ApiResponses::courseVersion);
    }

    @GetMapping("/versions/{versionId}")
    public ApiResponses.CourseVersionResponse version(@PathVariable Integer versionId, Authentication authentication) {
        accessService.requireCourseVersionOwner(authentication, versionId);
        return ApiResponses.courseVersion(courseService.getVersion(versionId));
    }

    @PostMapping("/templates/{templateId}/versions")
    public ApiResponses.CourseVersionResponse createVersion(@PathVariable Integer templateId,
                                                            @Valid @RequestBody CourseVersionRequest request,
                                                            Authentication authentication) {
        accessService.requireCourseTemplateOwner(authentication, templateId);
        Integer actorPersonId = accessService.currentPersonId(authentication);
        return ApiResponses.courseVersion(courseService.createVersion(
                templateId,
                request.versionNumber(),
                request.title(),
                request.description(),
                actorPersonId,
                request.published(),
                request.published() ? actorPersonId : null,
                request.changeNotes()
        ));
    }

    @PutMapping("/versions/{versionId}")
    public ApiResponses.CourseVersionResponse updateVersion(@PathVariable Integer versionId,
                                                            @Valid @RequestBody CourseVersionUpdateRequest request,
                                                            Authentication authentication) {
        accessService.requireCourseVersionOwner(authentication, versionId);
        return ApiResponses.courseVersion(courseService.updateVersion(
                versionId,
                request.versionNumber(),
                request.title(),
                request.description(),
                accessService.currentPersonId(authentication),
                request.changeNotes()
        ));
    }

    public record CourseTemplateRequest(
            @NotNull Integer subjectId,
            @NotBlank @Size(max = 200) String name,
            boolean publicVisible
    ) {
    }

    public record CourseVersionRequest(
            @Positive int versionNumber,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            boolean published,
            @Size(max = 2000) String changeNotes
    ) {
    }

    public record CourseVersionUpdateRequest(
            @Positive int versionNumber,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @Size(max = 2000) String changeNotes
    ) {
    }

    public record PublishVersionRequest() {
    }
    }
