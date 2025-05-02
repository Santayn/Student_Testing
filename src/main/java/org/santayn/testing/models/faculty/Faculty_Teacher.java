package org.santayn.testing.models.faculty;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.teacher.Teacher;

import java.util.List;

@Table
@Entity
@Data
public class Faculty_Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @ManyToOne
    @JoinColumn(name = "teacher_id", referencedColumnName = "id") // Внешний ключ на таблицу Teacher
    private Teacher teacher;
    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "id") // Внешний ключ на таблицу Faculty
    private Faculty faculty;
}
