package org.santayn.testing.models.group;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher_Group;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_group")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include  // Используем только id для equals/hashCode
    private Integer id;

    private String facultet;
    private Integer course_code;

    @Column(unique = true)
    private String name;

    private String title;

    // Связь OneToMany с Student через промежуточную таблицу
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group_Student> groupStudents = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "id")
    private Faculty faculty;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Group> teacherGroups = new ArrayList<>();

    public void addStudent(Student student) {
        Group_Student groupStudent = new Group_Student();
        groupStudent.setGroup(this);
        groupStudent.setStudent(student);
        groupStudents.add(groupStudent);
    }
}