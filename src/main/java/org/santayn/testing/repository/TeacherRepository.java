package org.santayn.testing.repository;

import org.santayn.testing.models.teacher.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

    // Получить всех студентов, принадлежащих к группе с указанным group_id
    @Query("SELECT tg.group FROM Teacher_Group tg WHERE tg.teacher.id = :teacher_id")
    List<Teacher> findGroupByTeacherId(@Param("teacher_id") Integer teacher_id);

    // Получить всех студентов
    @Query("SELECT t FROM Teacher t")
    List<Teacher> findAllTeachers();

    // Получить студента по его ID
    @Query("SELECT t FROM Teacher t WHERE t.id = :teacher_id")
    Optional<Teacher> findTeacherById(@Param("teacher_id") Integer teacher_id);

    // Получить студентов, которые не состоят ни в одной из групп
    @Query("""
        SELECT t 
        FROM Teacher t 
        WHERE t.id NOT IN (
            SELECT tg.teacher.id 
            FROM Teacher_Group tg
        )
    """)
    List<Teacher> findTeachersNotInAnyGroup();
}