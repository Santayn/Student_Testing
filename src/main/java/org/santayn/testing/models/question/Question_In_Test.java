package org.santayn.testing.models.question;

import jakarta.persistence.*;
import org.santayn.testing.models.test.Test;

@Entity
@Table
public class Question_In_Test {
    @Id
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "test_id", referencedColumnName = "id") // Внешний ключ на таблицу Test
    private Test test;

    @ManyToOne
    @JoinColumn(name = "question_id", referencedColumnName = "id") // Внешний ключ на таблицу Question
    private Question question;

    // Геттеры и сеттеры
    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }
}