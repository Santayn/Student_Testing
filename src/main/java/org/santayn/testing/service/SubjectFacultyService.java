package org.santayn.testing.service;

import jakarta.transaction.Transactional;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.Subject_Faculty;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.teacher.Teacher_Subject;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.SubjectFacultyRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubjectFacultyService {

    private final SubjectFacultyRepository subjectFacultyRepository;
    private final FacultyService facultyService;
    private final SubjectService subjectService;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;

    public SubjectFacultyService(FacultyRepository facultyRepository,SubjectRepository subjectRepository, SubjectFacultyRepository subjectFacultyRepository, FacultyService facultyService, SubjectService subjectService) {
        this.subjectFacultyRepository = subjectFacultyRepository;
        this.facultyService = facultyService;
        this.subjectService = subjectService;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
    }

    // Добавить связи между факультетом и предметами
    @Transactional
    public void addSubjectsToFaculty(Integer facultyId, List<Integer> subjectIds) {
        Faculty faculty = facultyService.findById(facultyId);

        for (Integer subjectId : subjectIds) {
            Subject subject = subjectService.findById(subjectId);

            Subject_Faculty subjectFaculty = new Subject_Faculty();
            subjectFaculty.setFaculty(faculty);
            subjectFaculty.setSubject(subject);

            subjectFacultyRepository.save(subjectFaculty);
        }
    }

    // Удалить связи между факультетом и предметами
    @Transactional
    public void removeSubjectsFromFaculty(Integer facultyId, List<Integer> subjectIds) {
        for (Integer subjectId : subjectIds) {
            subjectFacultyRepository.deleteByFacultyIdAndSubjectId(facultyId, subjectId);
        }
    }

    // Получение свободных студентов (не входящих в указанную группу)
    @Transactional
    public List<Subject> findFreeSubjects() {
        List<Subject> subject = subjectFacultyRepository.findSubjectsNotInAnyFaculty();
        System.out.println("Free subjects found: " + subject.size());
        return subject;
    }
    public Faculty deleteSubjectsFromFaculty(Integer facultyId, List<Integer> subjectIds) {
        // 1. Получаем преподавателя
        Faculty faculty = getFacultyById(facultyId);

        // 2. Для каждого subjectId находим связь "Teacher_Subject" и удаляем её
        for (Integer subjectId : subjectIds) {
            // Находим конкретную связь "Subject_Faculty"
            Subject_Faculty existingLink = subjectFacultyRepository.findByFacultyIdAndSubjectId(facultyId, subjectId)
                    .orElseThrow(() -> new RuntimeException("Связь между предметом и преподавателем не найдена"));

            // Удаляем эту связь
            subjectFacultyRepository.delete(existingLink);
        }

        // 3. Возвращаем обновлённого преподавателя
        return faculty;
    }
    public Faculty getFacultyById(Integer facultyId) {
        return facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("facultyId не найдена"));
    }

}