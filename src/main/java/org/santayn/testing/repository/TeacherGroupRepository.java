package org.santayn.testing.repository;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeacherGroupRepository extends JpaRepository<Teacher_Group, Integer> {

    // Получить группы, где препод закреплён, по логину пользователя
    @Query("""
           select tg.group
           from Teacher_Group tg
             join tg.teacher t
             join t.user u
           where u.login = :login
           """)
    List<Group> findGroupsByTeacherLogin(String login);
}
