package org.santayn.testing.models.faculty;

import jakarta.persistence.*;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.teacher.Teacher;

import java.util.List;

@Table
@Entity
public class Faculty_Teacher {
    @Id
    Integer id;
    @ManyToOne
    @JoinColumn(name = "teacher_id", referencedColumnName = "id") // Внешний ключ на таблицу Teacher
    private Teacher teacher;
    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "id") // Внешний ключ на таблицу Faculty
    private Faculty faculty;
}
