package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.service.CreateFacultyService;
import org.santayn.testing.service.FacultyService;
import org.santayn.testing.web.dto.faculty.CreateFacultyRequest;
import org.santayn.testing.web.dto.faculty.FacultyDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faculties")
public class FacultyRestController {

    private final FacultyService facultyService;
    private final CreateFacultyService createFacultyService;

    public FacultyRestController(FacultyService facultyService,
                                 CreateFacultyService createFacultyService) {
        this.facultyService = facultyService;
        this.createFacultyService = createFacultyService;
    }

    /** Список факультетов */
    @GetMapping
    public List<FacultyDto> list() {
        return facultyService.getAllFaculty()
                .stream()
                .map(FacultyDto::from)
                .toList();
    }

    /** Создать факультет */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyDto create(@RequestBody CreateFacultyRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Поле 'name' обязательно");
        }
        createFacultyService.addFaculty(req.name().trim());

        // На текущей бизнес-логике получаем созданный через поиск по имени (лучше доработать сервис, чтобы возвращал сущность/ID)
        Faculty created = facultyService.getAllFaculty().stream()
                .filter(f -> f.getName().equals(req.name().trim()))
                .reduce((first, second) -> second) // последний
                .orElseThrow();

        return FacultyDto.from(created);
    }

    /** Удалить факультет */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        createFacultyService.deleteFaculty(id);
    }
}
