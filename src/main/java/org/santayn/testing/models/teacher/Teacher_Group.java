package org.santayn.testing.models.teacher;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.student.Student;

import java.util.List;

@Table
@Entity
@Data
public class Teacher_Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @ManyToOne
    @JoinColumn(name = "teacher_id", referencedColumnName = "id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private Group group;

}
