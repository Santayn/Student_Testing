package org.santayn.testing.models.group;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.santayn.testing.models.test.Test_Group;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_group")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    private Integer course_code;

    @Column(unique = true)
    private String name;

    private String title;

    // Связь с студентами через промежуточную таблицу
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group_Student> groupStudents = new ArrayList<>();

    // Связь с тестами через Test_Group
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Test_Group> testGroups = new ArrayList<>();

    // Группа принадлежит факультету
    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "id")
    private Faculty faculty;

    // Преподаватели, связанные с группой
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Group> teacherGroups = new ArrayList<>();

    // Методы для управления студентами
    public void addStudent(Student student) {
        Group_Student gs = new Group_Student();
        gs.setGroup(this);
        gs.setStudent(student);
        student.setGroup(this);
        groupStudents.add(gs);
    }

    public void removeStudent(Student student) {
        groupStudents.stream()
                .filter(gs -> gs.getStudent().getId().equals(student.getId()))
                .findFirst()
                .ifPresent(gs -> {
                    gs.getStudent().setGroup(null);
                    groupStudents.remove(gs);
                });
    }
}