package org.santayn.testing.repository;
import java.util.List;
import java.util.Optional;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.santayn.testing.models.teacher.Teacher_Subject;
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
    List<Teacher_Group> findByTeacherId(Integer teacherId);

    @Query("""
    select tg.group
    from Teacher_Group tg
    join tg.teacher t
    join t.user u
    where u.login = :login
    """)
    List<Group> findGroupsByTeacherLogin(@Param("login") String login);
}