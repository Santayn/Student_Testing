package org.santayn.testing.models.teacher;

import jakarta.persistence.*;
import org.santayn.testing.models.faculty.Faculty_Teacher;
import org.santayn.testing.models.user.User;

import java.util.List;

@Table
@Entity
public class Teacher {
    @Id
    Integer id;
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    Integer facultet;
    Integer experience;
    Integer group_id;
    // Связь OneToMany с Teacher_Group
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Group> teacherGroups;

    // Связь OneToMany с Faculty_Teacher
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Faculty_Teacher> facultyTeachers;

    // Связь OneToMany с Teacher_Subject
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Subject> teacherSubjects;

}
