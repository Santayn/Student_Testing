package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.FacultySubjectService;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping({"/api/subjects", "/api/v1/subjects"})
public class SubjectRestController {

    private final SubjectService subjectService;
    private final FacultySubjectService facultySubjectService;

    public SubjectRestController(SubjectService subjectService,
                                 FacultySubjectService facultySubjectService) {
        this.subjectService = subjectService;
        this.facultySubjectService = facultySubjectService;
    }

    @GetMapping
    public List<ApiResponses.SubjectResponse> all(@RequestParam(required = false) Integer facultyId) {
        return ApiResponses.list(
                facultyId == null ? subjectService.findAll() : facultySubjectService.findSubjectsByFaculty(facultyId),
                ApiResponses::subject
        );
    }

    @GetMapping("/{id}")
    public ApiResponses.SubjectResponse one(@PathVariable Integer id) {
        return ApiResponses.subject(subjectService.get(id));
    }

    @GetMapping("/{id}/faculties")
    public List<ApiResponses.FacultyResponse> faculties(@PathVariable Integer id) {
        return ApiResponses.list(facultySubjectService.findFacultiesBySubject(id), ApiResponses::faculty);
    }

    @PostMapping
    public ApiResponses.SubjectResponse create(@Valid @RequestBody SubjectRequest request) {
        return ApiResponses.subject(subjectService.create(request.name(), request.description()));
    }

    @PutMapping("/{id}")
    public ApiResponses.SubjectResponse update(@PathVariable Integer id, @Valid @RequestBody SubjectRequest request) {
        return ApiResponses.subject(subjectService.update(id, request.name(), request.description()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        subjectService.delete(id);
    }

    public record SubjectRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description
    ) {
    }
}
