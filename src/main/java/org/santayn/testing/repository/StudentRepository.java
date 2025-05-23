    package org.santayn.testing.repository;
    import org.santayn.testing.models.student.Student;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Modifying;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;
    import java.util.List;
    import java.util.Optional;
    @Repository
    public interface StudentRepository extends JpaRepository<Student, Integer> {
        // Получить всех студентов, принадлежащих к группе с указанным group_id
        @Query("SELECT sg.student FROM Group_Student sg WHERE sg.group.id = :group_id")
        List<Student> findStudentByGroupId(@Param("group_id") Integer group_id);
        // Получить всех студентов
        @Query("SELECT s FROM Student s")
        List<Student> findAllStudents();
        // Получить студента по его ID
        @Query("SELECT s FROM Student s WHERE s.id = :student_id")
        Optional<Student> findStudentById(@Param("student_id") Integer student_id);
        // Получить студентов, которые не состоят ни в одной из групп
        @Query("""
            SELECT s 
            FROM Student s 
            WHERE s.id NOT IN (
                SELECT gs.student.id 
                FROM Group_Student gs
            )
        """)
        List<Student> findStudentsNotInAnyGroup();
        @Query("SELECT s FROM Student s WHERE s.user.id = :userId")
        Optional<Student> findStudentByUserId(@Param("userId") Integer userId);

        @Modifying
        @Query("DELETE FROM Student s WHERE s.user.id = :userId")
        void deleteByUser_Id(@Param("userId") Integer userId);
    }
