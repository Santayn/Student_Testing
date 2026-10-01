package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.FacultySubjectService;
import org.santayn.testing.service.FacultyService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/faculties")
public class FacultyRestController {

    private final FacultyService facultyService;
    private final FacultySubjectService facultySubjectService;

    public FacultyRestController(FacultyService facultyService,
                                 FacultySubjectService facultySubjectService) {
        this.facultyService = facultyService;
        this.facultySubjectService = facultySubjectService;
    }

    @GetMapping
    public List<ApiResponses.FacultyResponse> all() {
        return ApiResponses.list(facultyService.findAll(), ApiResponses::faculty);
    }

    @GetMapping("/{id}")
    public ApiResponses.FacultyResponse one(@PathVariable Integer id) {
        return ApiResponses.faculty(facultyService.get(id));
    }

    @GetMapping("/{id}/subjects")
    public List<ApiResponses.SubjectResponse> subjects(@PathVariable Integer id) {
        return ApiResponses.list(facultySubjectService.findSubjectsByFaculty(id), ApiResponses::subject);
    }

    @PostMapping
    public ApiResponses.FacultyResponse create(@Valid @RequestBody FacultyRequest request) {
        return ApiResponses.faculty(facultyService.create(request.name(), request.code(), request.description()));
    }

    @PutMapping("/{id}")
    public ApiResponses.FacultyResponse update(@PathVariable Integer id, @Valid @RequestBody FacultyRequest request) {
        return ApiResponses.faculty(facultyService.update(id, request.name(), request.code(), request.description()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        facultyService.delete(id);
    }

    @PutMapping("/{facultyId}/subjects")
    public List<ApiResponses.SubjectResponse> replaceSubjects(
            @PathVariable Integer facultyId,
            @Valid @RequestBody FacultySubjectsRequest request
    ) {
        return ApiResponses.list(
                facultySubjectService.replaceSubjects(facultyId, request.subjectIds()),
                ApiResponses::subject
        );
    }

    @PostMapping("/{facultyId}/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void linkSubject(@PathVariable Integer facultyId,
                            @PathVariable Integer subjectId) {
        facultySubjectService.link(facultyId, subjectId);
    }

    @DeleteMapping("/{facultyId}/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkSubject(@PathVariable Integer facultyId,
                              @PathVariable Integer subjectId) {
        facultySubjectService.unlink(facultyId, subjectId);
    }

    public record FacultySubjectsRequest(
            Set<@Positive Integer> subjectIds
    ) {
        public FacultySubjectsRequest {
            subjectIds = subjectIds == null ? Set.of() : Set.copyOf(subjectIds);
        }
    }

    public record FacultyRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 50) String code,
            @Size(max = 1000) String description
    ) {
    }
}
