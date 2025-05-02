package org.santayn.testing.models.test;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;

@Table
@Entity
@Data
public class Test_Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @OneToOne
    @JoinColumn(name = "lecture_id", referencedColumnName = "id") // Внешний ключ на таблицу app_user
    private Lecture lecture;
    @OneToOne
    @JoinColumn(name = "test_id", referencedColumnName = "id") // Внешний ключ на таблицу app_user
    private Test test;
}
