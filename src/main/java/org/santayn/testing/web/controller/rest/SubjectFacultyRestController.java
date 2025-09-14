package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.service.SubjectFacultyService;
import org.santayn.testing.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faculty-subjects")
public class SubjectFacultyRestController {

    private final SubjectService subjectService;
    private final SubjectFacultyService subjectFacultyService;

    public SubjectFacultyRestController(SubjectService subjectService,
                                        SubjectFacultyService subjectFacultyService) {
        this.subjectService = subjectService;
        this.subjectFacultyService = subjectFacultyService;
    }

    /** Предметы факультета */
    @GetMapping("/{facultyId}")
    public List<SubjectDto> getSubjectsOfFaculty(@PathVariable Integer facultyId) {
        return subjectService.getSubjectsByFacultyId(facultyId).stream()
                .map(SubjectDto::from)
                .toList();
    }

    /** Свободные предметы */
    @GetMapping("/free")
    public List<SubjectDto> getFreeSubjects(@RequestParam Integer facultyId) {
        return subjectFacultyService.findFreeSubjectsFromFaculty().stream()
                .map(SubjectDto::from)
                .toList();
    }

    /** Добавить предметы к факультету */
    @PostMapping("/{facultyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addSubjects(@PathVariable Integer facultyId,
                            @RequestBody List<Integer> subjectIds) {
        subjectFacultyService.addSubjectsToFaculty(facultyId, subjectIds);
    }

    /** Удалить предметы из факультета */
    @DeleteMapping("/{facultyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSubjects(@PathVariable Integer facultyId,
                               @RequestBody List<Integer> subjectIds) {
        subjectFacultyService.deleteSubjectsFromFaculty(facultyId, subjectIds);
    }

    // DTO
    public record SubjectDto(Integer id, String name) {
        static SubjectDto from(Subject s){ return new SubjectDto(s.getId(), s.getName()); }
    }
}
