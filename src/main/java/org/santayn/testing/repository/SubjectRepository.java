package org.santayn.testing.repository;

import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.test.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Test, Integer> {
    @Query("SELECT sf.subject FROM Subject_Faculty sf WHERE sf.faculty.id = :faculty_id")
    List<Subject> findSubjectByFacultyId(@Param("faculty_id") Integer faculty_id);
}