package org.santayn.testing.models.question;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.test.Test;

@Entity
@Table
@Data
public class Question_In_Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "test_id", referencedColumnName = "id") // Внешний ключ на таблицу Test
    private Test test;

    @ManyToOne
    @JoinColumn(name = "question_id", referencedColumnName = "id") // Внешний ключ на таблицу Question
    private Question question;
}