package org.santayn.testing.repository;
import java.util.Optional;

import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface Teacher_GroupRepository extends JpaRepository<Teacher_Group, Long> {
    Optional<Teacher_Group> findByTeacherIdAndGroupId(Integer teacherId, Integer groupId);

    @Modifying
    @Query(value = "DELETE FROM teacher_group WHERE teacher_id = :teacherId", nativeQuery = true)
    void deleteByTeacherId(@Param("teacherId") Integer teacherId);
}