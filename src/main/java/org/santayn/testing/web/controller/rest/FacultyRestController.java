package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.service.FacultyService;
import org.santayn.testing.service.CreateFacultyService;
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

    // Список
    @GetMapping
    public List<FacultyResponse> list() {
        return facultyService.getAllFaculty().stream()
                .map(f -> new FacultyResponse(f.getId(), f.getName()))
                .toList();
    }

    // Создать
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyResponse create(@RequestBody CreateFacultyRequest req) {
        createFacultyService.addFaculty(req.name());
        // Возвращаем созданный (берём последний по имени, либо можно доработать сервис, чтобы вернуть ID)
        Faculty created = facultyService.getAllFaculty().stream()
                .filter(f -> f.getName().equals(req.name()))
                .reduce((first, second) -> second) // последний
                .orElseThrow();
        return new FacultyResponse(created.getId(), created.getName());
    }

    // Удалить
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        createFacultyService.deleteFaculty(id);
    }

    // DTO
    public record FacultyResponse(Integer id, String name) {}
    public record CreateFacultyRequest(String name) {}
}
