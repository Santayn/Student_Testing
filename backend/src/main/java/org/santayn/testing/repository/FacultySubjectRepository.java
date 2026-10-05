package org.santayn.testing.repository;

import org.santayn.testing.models.subject.FacultySubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultySubjectRepository extends JpaRepository<FacultySubject, Integer> {

    List<FacultySubject> findByFacultyId(Integer facultyId);

    List<FacultySubject> findBySubjectId(Integer subjectId);

    Optional<FacultySubject> findByFacultyIdAndSubjectId(Integer facultyId, Integer subjectId);

    boolean existsByFacultyIdAndSubjectId(Integer facultyId, Integer subjectId);

    void deleteByFacultyIdAndSubjectId(Integer facultyId, Integer subjectId);
}
