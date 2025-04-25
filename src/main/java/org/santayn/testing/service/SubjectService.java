
package org.santayn.testing.service;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<Subject> getSubjectByID(Integer facultyId) {
        if (facultyId == null) {
            throw new IllegalArgumentException("Test ID cannot be null");
        }
        return subjectRepository.findSubjectByFacultyId(facultyId);
    }


    public Subject getSpecificSubjectByFacultyIdAndSubjectId(Integer facultyId, Integer subjectId) {

        List<Subject> subjects = subjectRepository.findSubjectByFacultyId(facultyId);

        Optional<Subject> specificSubject = subjects.stream()
                .filter(subject -> subject.getId().equals(facultyId))
                .findFirst();

        return specificSubject.orElseThrow(() -> new RuntimeException(
                "Question with ID " + subjectId + " not found in Test with ID " + facultyId));
    }
}