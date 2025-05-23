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
    @EqualsAndHashCode.Include
    private Integer id;

    private Integer course_code;
    @Column(unique = true)
    private String name;
    private String title;

    // OneToMany: студенты через промежуточную таблицу
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group_Student> groupStudents = new ArrayList<>();

    // ManyToOne: группа относится к одному факультету
    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "id")
    private Faculty faculty;

    // OneToMany: преподаватели, связанные с этой группой
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Group> teacherGroups = new ArrayList<>();

    public void addStudent(Student student) {
        Group_Student groupStudent = new Group_Student();
        groupStudent.setGroup(this);
        groupStudent.setStudent(student);

        // Устанавливаем группу у студента
        student.setGroup(this);

        groupStudents.add(groupStudent);
    }
    public void removeStudent(Student student) {
        Group_Student toRemove = null;
        for (Group_Student gs : groupStudents) {
            if (gs.getStudent().getId().equals(student.getId())) {
                toRemove = gs;
                break;
            }
        }

        if (toRemove != null) {
            // Очищаем поле group у студента
            toRemove.getStudent().setGroup(null);
            groupStudents.remove(toRemove);
        }
    }
}