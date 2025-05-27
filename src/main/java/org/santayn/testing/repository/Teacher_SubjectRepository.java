package org.santayn.testing.repository;

import org.santayn.testing.models.teacher.Teacher_Group;
import org.santayn.testing.models.teacher.Teacher_Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface Teacher_SubjectRepository extends JpaRepository<Teacher_Subject, Integer> {
    List<Teacher_Subject> findByTeacherId(Integer teacherId);
    Optional<Teacher_Subject> findByTeacherIdAndSubjectId(Integer teacherId, Integer subjectId);
    void deleteByTeacherIdAndSubjectId(Integer teacherId, Integer subjectId);

    @Modifying
    @Query(value = "DELETE FROM teacher_subject WHERE teacher_id = :teacherId", nativeQuery = true)
    void deleteByTeacherId(@Param("teacherId") Integer teacherId);
}