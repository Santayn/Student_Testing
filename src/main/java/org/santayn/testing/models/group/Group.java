package org.santayn.testing.models.group;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.santayn.testing.models.user.User;

import java.util.ArrayList;
import java.util.List;

@Table(name = "app_group")
@Entity
@Data
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    String facultet;
    Integer course_code;
    @Column(unique = true)
    String name;
    String title;

    // Связь OneToMany с Student через промежуточную таблицу
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group_Student> group_students;
    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "id")
    private Faculty faculty;
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Group> teacherGroups;
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "group_students",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private List<Student> students = new ArrayList<>();

    public void addStudent(Student student) {
        students.add(student);
    }
}