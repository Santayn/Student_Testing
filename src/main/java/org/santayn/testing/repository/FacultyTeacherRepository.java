package org.santayn.testing.repository;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.faculty.Faculty_Teacher;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacultyTeacherRepository extends Repository<Faculty_Teacher, Integer> {
    @Query("""
        select ft.faculty
        from Faculty_Teacher ft
        join ft.teacher t
        join t.user u
        where u.login = :login
        """)
    List<Faculty> findFacultiesByTeacherLogin(@Param("login") String teacherLogin);
}
