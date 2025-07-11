package org.santayn.testing.repository;

import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.Subject_Faculty;
import org.santayn.testing.models.teacher.Teacher_Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectFacultyRepository extends JpaRepository<Subject_Faculty, Long> {
    void deleteByFacultyIdAndSubjectId(Integer facultyId, Integer subjectId);

    @Query("""
        SELECT s
        FROM Subject s
        WHERE s.id NOT IN (
            SELECT sf.subject.id
            FROM Subject_Faculty sf
        )
    """)
    List<Subject> findSubjectsNotInAnyFaculty();
    Optional<Subject_Faculty> findByFacultyIdAndSubjectId(Integer facultyId, Integer subjectId);
}
