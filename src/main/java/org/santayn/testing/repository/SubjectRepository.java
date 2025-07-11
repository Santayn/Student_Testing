package org.santayn.testing.repository;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.teacher.Teacher_Subject; // Добавлен импорт
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {

    @Query("SELECT sf.subject FROM Subject_Faculty sf WHERE sf.faculty.id = :faculty_id")
    List<Subject> findSubjectsNotInAnyFaculty (@Param("faculty_id") Integer faculty_id);
    @Query("SELECT sf.subject FROM Subject_Faculty sf WHERE sf.faculty.id = :faculty_id")
    List<Subject> findSubjectByFacultyId(@Param("faculty_id") Integer faculty_id);

    @Query("SELECT s FROM Subject s")
    List<Subject> findAllSubjects();

    @Query("SELECT s FROM Subject s WHERE s.name = :subject_name")
    List<Subject> findSubjectByName(@Param("subject_name") String subject_name);

    @Query("SELECT s FROM Subject s WHERE s.id = :subjectId")
    Optional<Subject> findSubjectById(@Param("subjectId") Integer subjectId);

    @Query("""
        SELECT s 
        FROM Subject s 
        WHERE s.id NOT IN ( 
            SELECT ts.subject.id 
            FROM Teacher_Subject ts
        )
    """)
    List<Subject> findSubjectsNotInAnyTeachers();

    @Query("SELECT ts.subject FROM Teacher_Subject ts WHERE ts.teacher.id = :teacher_id")
    List<Subject> findSubjectByTeacherId(@Param("teacher_id") Integer teacher_id);
}