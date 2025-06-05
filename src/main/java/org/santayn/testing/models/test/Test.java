package org.santayn.testing.models.test;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.question.Question_In_Test;
import org.santayn.testing.models.question.Question_Test_Answer;
import org.santayn.testing.models.topic.Topic;

import java.util.List;

@Entity
@Table(name = "test")
@Data
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Тема, из которой будут браться вопросы
    @ManyToOne
    @JoinColumn(name = "topic_id", referencedColumnName = "id")
    private Topic topic;

    // Количество вопросов в тесте
    private int questionCount;

    // Описание теста (необязательно)
    private String description;
}