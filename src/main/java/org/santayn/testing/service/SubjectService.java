package org.santayn.testing.service;
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
}