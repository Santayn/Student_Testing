package org.santayn.testing.models.subject;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;

@Entity
@Table(name = "subject_lecture") // Указываем явное имя таблицы в БД (по желанию)
@Data
public class Subject_Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Связь с Subject
    @OneToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id")
    private Subject subject;

    // Связь с Lecture
    @OneToOne
    @JoinColumn(name = "lecture_id", referencedColumnName = "id")
    private Lecture lecture;

}