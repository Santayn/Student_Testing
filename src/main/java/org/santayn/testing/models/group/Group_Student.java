package org.santayn.testing.models.group;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher;

import java.util.List;

@Entity
@Data
@Table(name = "group_student")
public class Group_Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "id")
    private Student student;
}