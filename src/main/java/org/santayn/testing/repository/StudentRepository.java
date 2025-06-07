package org.santayn.testing.repository;

import org.santayn.testing.models.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    // 1) Поиск студентов по ID группы
    @Query("SELECT sg.student FROM Group_Student sg WHERE sg.group.id = :groupId")
    List<Student> findStudentByGroupId(@Param("groupId") Integer groupId);

    // 2) Все студенты (по сути дублирует findAll())
    @Query("SELECT s FROM Student s")
    List<Student> findAllStudents();

    // 3) Поиск по собственному ID (дублирует findById())
    @Query("SELECT s FROM Student s WHERE s.id = :studentId")
    Optional<Student> findStudentById(@Param("studentId") Integer studentId);

    // 4) Студенты без группы
    @Query("""
        SELECT s 
        FROM Student s 
        WHERE s.id NOT IN (
            SELECT gs.student.id 
            FROM Group_Student gs
        )
    """)
    List<Student> findStudentsNotInAnyGroup();

    // 5) Поиск по связанному пользователю (ID)
    @Query("SELECT s FROM Student s WHERE s.user.id = :userId")
    Optional<Student> findStudentByUserId(@Param("userId") Integer userId);

    // 6) Поиск текущего студента по логину (для Spring Security principal.getName())
    @Query("SELECT s FROM Student s WHERE s.user.login = :login")
    Optional<Student> findByUserLogin(@Param("login") String login);

    // 7) Удаление по user.id
    @Transactional
    @Modifying
    @Query("DELETE FROM Student s WHERE s.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Integer userId);
}