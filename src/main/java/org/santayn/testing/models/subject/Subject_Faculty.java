package org.santayn.testing.models.subject;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.faculty.Faculty;


@Table
@Entity
@Data
public class Subject_Faculty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "id") // Внешний ключ на таблицу Faculty
    private Faculty faculty;

    @ManyToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id") // Внешний ключ на таблицу Subject
    private Subject subject;
}
