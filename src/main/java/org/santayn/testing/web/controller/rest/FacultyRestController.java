package org.santayn.testing.web.controller.rest;

import org.santayn.testing.service.FacultyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faculties")
public class FacultyRestController {
    private final FacultyService facultyService;
    public FacultyRestController(FacultyService facultyService){ this.facultyService = facultyService; }

    @GetMapping
    public List<FacultyResponse> list(){
        return facultyService.getAllFaculty().stream()
                .map(f -> new FacultyResponse(f.getId(), f.getName(), f.getDescription()))
                .toList();
    }

    public record FacultyResponse(Integer id, String name, String description) {}
}
