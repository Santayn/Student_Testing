package org.santayn.testing.service;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.Group_StudentRepository;
import org.santayn.testing.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;


    public FacultyService(FacultyRepository facultyRepository)
    {
        this.facultyRepository = facultyRepository;
    }
    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAllFacultys();
    }
    public Faculty findById(Integer id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + id));
    }
}
