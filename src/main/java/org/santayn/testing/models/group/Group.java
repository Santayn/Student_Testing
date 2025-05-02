package org.santayn.testing.models.group;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher_Group;

import java.util.List;

@Table(name = "app_group")
@Entity
@Data
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    Integer facultet;
    Integer course_code;
    String name;
    String title;

    // Связь OneToMany с Student через промежуточную таблицу
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group_Student> group_students;

    // Связь OneToMany с Teacher_Group
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Group> teacherGroups;

}