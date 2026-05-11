package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.LectureService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping({"/api/lectures", "/api/v1/lectures"})
public class LectureRestController {

    private final LectureService lectureService;

    public LectureRestController(LectureService lectureService) {
        this.lectureService = lectureService;
    }

    @GetMapping
    public List<ApiResponses.LectureResponse> all(@RequestParam(required = false) Integer subjectId,
                                                  @RequestParam(required = false) Integer subjectMembershipId,
                                                  @RequestParam(required = false) Integer courseVersionId) {
        return ApiResponses.list(
                lectureService.findAll(subjectId, subjectMembershipId, courseVersionId),
                ApiResponses::lecture
        );
    }

    @GetMapping("/{id}")
    public ApiResponses.LectureResponse one(@PathVariable Integer id) {
        return ApiResponses.lecture(lectureService.get(id));
    }

    @PostMapping
    public ApiResponses.LectureResponse create(@Valid @RequestBody LectureRequest request) {
        return ApiResponses.lecture(lectureService.create(
                request.subjectId(),
                request.subjectMembershipId(),
                request.courseVersionId(),
                request.ordinal(),
                request.title(),
                request.description(),
                request.contentFolderKey(),
                request.linkedTestId(),
                request.publicVisible()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponses.LectureResponse update(@PathVariable Integer id, @Valid @RequestBody LectureRequest request) {
        return ApiResponses.lecture(lectureService.update(
                id,
                request.subjectId(),
                request.subjectMembershipId(),
                request.courseVersionId(),
                request.ordinal(),
                request.title(),
                request.description(),
                request.contentFolderKey(),
                request.linkedTestId(),
                request.publicVisible()
        ));
    }

    @PutMapping("/{id}/linked-test")
    public ApiResponses.LectureResponse updateLinkedTest(@PathVariable Integer id,
                                                         @Valid @RequestBody LinkedTestRequest request) {
        return ApiResponses.lecture(lectureService.updateLinkedTest(id, request.linkedTestId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        lectureService.delete(id);
    }

    public record LectureRequest(
            Integer subjectId,
            Integer subjectMembershipId,
            Integer courseVersionId,
            @Positive int ordinal,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 255) String contentFolderKey,
            Integer linkedTestId,
            boolean publicVisible
    ) {
    }

    public record LinkedTestRequest(
            Integer linkedTestId
    ) {
    }
}
