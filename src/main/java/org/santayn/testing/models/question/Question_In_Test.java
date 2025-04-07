package org.santayn.testing.models.question;

import jakarta.persistence.*;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.test.Test;

@Table
@Entity
public class Question_In_Test {
    @Id
    Integer id;
    @ManyToOne
    @JoinColumn(name = "test_id", referencedColumnName = "id") // Внешний ключ на таблицу Test
    private Test test;
    @ManyToOne
    @JoinColumn(name = "question_id", referencedColumnName = "id") // Внешний ключ на таблицу Question
    private Question question;
}
