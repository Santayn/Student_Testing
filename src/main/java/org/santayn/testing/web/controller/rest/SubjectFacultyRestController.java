package org.santayn.testing.web.controller.rest;

import org.santayn.testing.service.SubjectFacultyService;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.web.dto.subject.SubjectShortDto;
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
    public List<SubjectShortDto> getSubjectsOfFaculty(@PathVariable Integer facultyId) {
        return subjectService.getSubjectsByFacultyId(facultyId)
                .stream()
                .map(SubjectShortDto::from)
                .toList();
    }

    /** Свободные предметы */
    @GetMapping("/free")
    public List<SubjectShortDto> getFreeSubjects(@RequestParam Integer facultyId) {
        return subjectFacultyService.findFreeSubjectsFromFaculty()
                .stream()
                .map(SubjectShortDto::from)
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
}
