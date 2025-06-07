package org.santayn.testing.models.question;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.topic.Topic;

@Entity
@Table(name = "question")
@Data
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Поле для хранения всего текста вопроса (включая варианты и ответ)
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    // Правильный ответ (буква: a, b, c и т.д.)
    @Column(name = "correct_answer")
    private String correctAnswer;

    // Связь с темой
    @ManyToOne
    @JoinColumn(name = "topic_id", referencedColumnName = "id")
    private Topic topic;
}