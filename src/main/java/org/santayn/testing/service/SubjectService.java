package org.santayn.testing.service;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }




    public List<Subject> findFreeSubjects() {
        List<Subject> subjects = subjectRepository.findSubjectsNotInAnyTeachers();
        System.out.println("Free subjects found: " + subjects.size());
        return subjects;
    }

    public List<Subject> getSubjectsByTecherID(Integer teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("teacherId не может быть null");
        }
        System.out.println("Fetching Subject for teacher ID: " + teacherId);
        List<Subject> subjects = subjectRepository.findSubjectByTeacherId(teacherId);
        System.out.println("Groups found: " + subjects.size());
        return subjects;
    }




    public Subject getSpecificSubjectByFacultyIdAndSubjectId(Integer facultyId, Integer subjectId) {

        List<Subject> subjects = subjectRepository.findSubjectByFacultyId(facultyId);

        Optional<Subject> specificSubject = subjects.stream()
                .filter(subject -> subject.getId().equals(facultyId))
                .findFirst();

        return specificSubject.orElseThrow(() -> new RuntimeException(
                "Question with ID " + subjectId + " not found in Test with ID " + facultyId));
    }
    public List<Subject> getSubjectsByFacultyId(Integer facultyId) {
        return subjectRepository.findSubjectByFacultyId(facultyId);
    }

    // Получение конкретного предмета по ID факультета и ID предмета
    public Subject findSubjectByIdAndFacultyId(Integer facultyId, Integer subjectId) {
        List<Subject> subjects = subjectRepository.findSubjectByFacultyId(facultyId);

        Optional<Subject> specificSubject = subjects.stream()
                .filter(subject -> subject.getId().equals(subjectId))
                .findFirst();

        return specificSubject.orElseThrow(() -> new RuntimeException(
                "Subject with ID " + subjectId + " not found for Faculty with ID " + facultyId));
    }


    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }
}