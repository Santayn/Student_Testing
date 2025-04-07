package org.santayn.testing.models.subject;

import jakarta.persistence.*;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.teacher.Teacher;

@Table
@Entity
public class Subject_Faculty {
    @Id
    Integer id;
    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "id") // Внешний ключ на таблицу Faculty
    private Faculty faculty;

    @ManyToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id") // Внешний ключ на таблицу Subject
    private Subject subject;
}
