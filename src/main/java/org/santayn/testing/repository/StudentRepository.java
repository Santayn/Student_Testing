package org.santayn.testing.repository;

import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.test.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    @Query("SELECT sg.student FROM Group_Student sg WHERE sg.group.id = :group_id")
    List<Student> findStudentByGroupId(@Param("group_id") Integer group_id);
    
}