package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.subject.FacultySubject;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.FacultySubjectRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacultySubjectService {

    private final FacultyRepository facultyRepository;
    private final SubjectRepository subjectRepository;
    private final FacultySubjectRepository facultySubjectRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;

    @Transactional(readOnly = true)
    public List<Subject> findSubjectsByFaculty(Integer facultyId) {
        requireFaculty(facultyId);
        return facultySubjectRepository.findByFacultyId(facultyId)
                .stream()
                .map(FacultySubject::getSubjectId)
                .distinct()
                .map(this::requireSubject)
                .sorted(Comparator.comparing(Subject::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Faculty> findFacultiesBySubject(Integer subjectId) {
        requireSubject(subjectId);
        return facultySubjectRepository.findBySubjectId(subjectId)
                .stream()
                .map(FacultySubject::getFacultyId)
                .distinct()
                .map(this::requireFaculty)
                .sorted(Comparator.comparing(Faculty::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean exists(Integer facultyId, Integer subjectId) {
        return facultySubjectRepository.existsByFacultyIdAndSubjectId(facultyId, subjectId);
    }

    @Transactional
    public FacultySubject link(Integer facultyId, Integer subjectId) {
        requireFaculty(facultyId);
        requireSubject(subjectId);
        if (facultySubjectRepository.existsByFacultyIdAndSubjectId(facultyId, subjectId)) {
            throw new AuthConflictException("Subject is already linked to faculty: " + facultyId + "/" + subjectId);
        }

        FacultySubject facultySubject = new FacultySubject();
        facultySubject.setFacultyId(facultyId);
        facultySubject.setSubjectId(subjectId);
        return facultySubjectRepository.save(facultySubject);
    }

    @Transactional
    public void unlink(Integer facultyId, Integer subjectId) {
        requireFaculty(facultyId);
        requireSubject(subjectId);
        if (!facultySubjectRepository.existsByFacultyIdAndSubjectId(facultyId, subjectId)) {
            throw new IllegalArgumentException("Faculty subject link not found: " + facultyId + "/" + subjectId);
        }
        if (teachingAssignmentRepository.existsByFacultyIdAndSubjectId(facultyId, subjectId)) {
            throw new IllegalArgumentException(
                    "Cannot unlink subject from faculty while teaching assignments exist: " + facultyId + "/" + subjectId
            );
        }
        facultySubjectRepository.deleteByFacultyIdAndSubjectId(facultyId, subjectId);
    }

    private Faculty requireFaculty(Integer facultyId) {
        return facultyRepository.findById(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyId));
    }

    private Subject requireSubject(Integer subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
    }
}
