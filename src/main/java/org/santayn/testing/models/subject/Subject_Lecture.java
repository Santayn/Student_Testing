package org.santayn.testing.models.subject;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.teacher.Teacher;

@Table
@Entity
@Data
public class Subject_Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @OneToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id") // Внешний ключ на таблицу app_user
    private Subject subject;
    @OneToOne
    @JoinColumn(name = "lecture_id", referencedColumnName = "id") // Внешний ключ на таблицу app_user
    private Lecture lecture;
}
