package org.santayn.testing.repository;

import java.util.Optional;
import org.santayn.testing.models.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    // --- твои служебные удалялки (оставил как были) ---
    @Modifying
    @Transactional
    @Query(value = "UPDATE student SET id_group = NULL WHERE id_group = :groupId", nativeQuery = true)
    void clearGroup_IdFromStudent(@Param("groupId") Integer groupId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM group_student WHERE group_id = :groupId", nativeQuery = true)
    void deleteGroupStudentByGroupId(@Param("groupId") Integer groupId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM teacher_group WHERE group_id = :groupId", nativeQuery = true)
    void deleteTeacherGroupByGroupId(@Param("groupId") Integer groupId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM app_group WHERE id = :groupId", nativeQuery = true)
    void deleteGroupById(@Param("groupId") Integer groupId);
}
