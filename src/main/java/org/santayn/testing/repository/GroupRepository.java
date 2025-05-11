package org.santayn.testing.repository;

import java.util.Optional;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.test.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {
    @Query("SELECT g FROM Group g")
    List<Group> findAllGroups();
    @Query("SELECT g FROM Group g WHERE g.name = :group_name")
    List<Group> findGroupByName(@Param("group_name") String group_name);
    @Query("SELECT g FROM Group g WHERE g.id = :groupId")
    Optional<Group> findGroupById(@Param("groupId") Integer groupId);
    @Query("""
        SELECT g 
        FROM Group g 
        WHERE g.id NOT IN (
            SELECT tg.group.id 
            FROM Teacher_Group tg
        )
    """)
    List<Group> findGroupsNotInAnyTeachers();
    @Query("SELECT tg.group FROM Teacher_Group tg WHERE tg.teacher.id = :teacher_id")
    List<Group> findGroupByTeacherId(@Param("teacher_id") Integer teacher_id);
}