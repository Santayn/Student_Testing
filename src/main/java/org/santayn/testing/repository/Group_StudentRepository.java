package org.santayn.testing.repository;
import java.util.Optional;

import org.santayn.testing.models.group.Group_Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Group_StudentRepository extends JpaRepository<Group_Student, Long> {
    Optional<Group_Student> findByGroupIdAndStudentId(Integer groupId, Integer studentId);
}