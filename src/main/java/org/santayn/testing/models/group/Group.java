package org.santayn.testing.models.group;

import jakarta.persistence.*;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher_Group;

import java.util.List;

@Table(name = "app_group")
@Entity
public class Group {
    @Id
    Integer id;

    Integer facultet;
    Integer course_code;
    String title;

    // Связь OneToMany с Student через промежуточную таблицу
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Student> students;

    // Связь OneToMany с Teacher_Group
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Group> teacherGroups;

    // Геттеры и сеттеры
}