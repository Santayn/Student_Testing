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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
    public List<Subject> replaceSubjects(Integer facultyId, Set<Integer> subjectIds) {
        facultyRepository.findByIdForUpdate(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyId));

        Set<Integer> requestedIds = subjectIds == null
                ? Set.of()
                : subjectIds.stream()
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Subject> requestedSubjects = subjectRepository.findAllById(requestedIds);
        Map<Integer, Subject> subjectsById = requestedSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));
        Set<Integer> missingSubjectIds = new LinkedHashSet<>(requestedIds);
        missingSubjectIds.removeAll(subjectsById.keySet());
        if (!missingSubjectIds.isEmpty()) {
            throw new IllegalArgumentException("Subjects not found: " + missingSubjectIds);
        }

        List<FacultySubject> existingLinks = facultySubjectRepository.findByFacultyId(facultyId);
        Map<Integer, FacultySubject> existingBySubjectId = existingLinks.stream()
                .collect(Collectors.toMap(FacultySubject::getSubjectId, Function.identity()));

        Set<Integer> removals = new HashSet<>(existingBySubjectId.keySet());
        removals.removeAll(requestedIds);

        for (Integer subjectId : removals) {
            if (teachingAssignmentRepository.existsByFacultyIdAndSubjectId(facultyId, subjectId)) {
                throw new IllegalArgumentException(
                        "Cannot unlink subject from faculty while teaching assignments exist: "
                                + facultyId + "/" + subjectId
                );
            }
        }

        Set<Integer> additions = new LinkedHashSet<>(requestedIds);
        additions.removeAll(existingBySubjectId.keySet());

        if (!removals.isEmpty()) {
            facultySubjectRepository.deleteAll(
                    removals.stream()
                            .map(existingBySubjectId::get)
                            .toList()
            );
            facultySubjectRepository.flush();
        }

        if (!additions.isEmpty()) {
            List<FacultySubject> newLinks = additions.stream()
                    .map(subjectId -> {
                        FacultySubject link = new FacultySubject();
                        link.setFacultyId(facultyId);
                        link.setSubjectId(subjectId);
                        return link;
                    })
                    .toList();
            facultySubjectRepository.saveAll(newLinks);
        }

        return requestedIds.stream()
                .map(subjectsById::get)
                .sorted(Comparator.comparing(Subject::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
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
