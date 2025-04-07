package org.santayn.testing.models.question;

import jakarta.persistence.*;
import org.santayn.testing.models.answer.Answer;

@Entity
@Table(name = "question_answers")
public class Question_Answer {
    @Id
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "question_id", referencedColumnName = "id") // Связь с таблицей questions
    private Question question;

    @ManyToOne
    @JoinColumn(name = "answer_id", referencedColumnName = "id") // Связь с таблицей answers
    private Answer answer;

    // Геттеры и сеттеры
}