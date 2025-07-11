package org.santayn.testing.service;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CreateFacultyService {

    private final FacultyRepository facultyRepository;


    public CreateFacultyService(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;

    }
    public Optional<Faculty> getFacultyById(Integer facultyId) {
        return facultyRepository.findById(facultyId);
    }

    public void addFaculty(String name ) {
        Faculty faculty = new Faculty();
        faculty.setName(name);
        facultyRepository.save(faculty);
    }

    public void deleteFaculty(Integer facultyId) {
        Optional<Faculty> faculty = facultyRepository.findById(facultyId);
        if (faculty.isPresent()) {
            facultyRepository.clearFacultyIdFromAppGroup(facultyId); // Очищаем faculty_id в app_group
            facultyRepository.deleteSubjectFacultyByFacultyId(facultyId); // Удаляем записи из subject_faculty
            facultyRepository.deleteFacultyById(facultyId); // Удаляем сам факультет
        } else {
            throw new RuntimeException("Факультет с ID " + facultyId + " не найден.");
        }
    }
    public List<Faculty> getAllFacultys() {
        return facultyRepository.findAll();
    }
}