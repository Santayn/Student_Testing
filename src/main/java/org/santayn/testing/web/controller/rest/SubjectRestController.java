package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.web.dto.subject.SubjectCreateRequest;
import org.santayn.testing.web.dto.subject.SubjectDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectRestController {

    private final SubjectService subjectService;
    private final SubjectRepository subjectRepository;

    public SubjectRestController(SubjectService subjectService,
                                 SubjectRepository subjectRepository) {
        this.subjectService = subjectService;
        this.subjectRepository = subjectRepository;
    }

    /** Все предметы */
    @GetMapping
    public List<SubjectDto> list() {
        return subjectService.getAllSubjects()
                .stream()
                .map(SubjectDto::from)
                .toList();
    }

    /**
     * Свободные предметы.
     * Параметр facultyId необязателен и игнорируется (оставлен для совместимости c фронтом).
     */
    @GetMapping("/free")
    public List<SubjectDto> free(@RequestParam(value = "facultyId", required = false) Integer ignoredFacultyId) {
        return subjectService.findFreeSubjects()
                .stream()
                .map(SubjectDto::from)
                .toList();
    }

    /** Предметы по учителю */
    @GetMapping("/by-teacher/{teacherId}")
    public List<SubjectDto> byTeacher(@PathVariable Integer teacherId) {
        return subjectService.getSubjectsByTecherID(teacherId)
                .stream()
                .map(SubjectDto::from)
                .toList();
    }

    /** Один предмет по id */
    @GetMapping("/{id}")
    public SubjectDto get(@PathVariable Integer id) {
        return SubjectDto.from(subjectService.findById(id));
    }

    /** Создать предмет */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubjectDto> create(@RequestBody SubjectCreateRequest body) {
        if (body == null || body.name() == null || body.name().isBlank()) {
            throw new IllegalArgumentException("Поле 'name' обязательно");
        }
        Subject s = new Subject();
        s.setName(body.name().trim());
        s.setDescription(body.description());
        Subject saved = subjectRepository.save(s);

        SubjectDto dto = SubjectDto.from(saved);
        return ResponseEntity
                .created(URI.create("/api/v1/subjects/" + saved.getId()))
                .body(dto);
    }

    /** Удалить предмет */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        subjectRepository.deleteById(id);
    }

    /* === 400 для ошибок бизнес-валидации === */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
