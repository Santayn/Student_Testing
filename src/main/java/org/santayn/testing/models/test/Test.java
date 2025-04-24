package org.santayn.testing.models.test;

import jakarta.persistence.*;
import org.santayn.testing.models.question.Question_In_Test;
import org.santayn.testing.models.question.Question_Test_Answer;

import java.util.List;

@Entity
@Table
public class Test {
    @Id
    private Integer id;
    private Integer item_id;
    private Integer question_id;
    private String topic;

    @OneToOne(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private Question_Test_Answer question_test_answer;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question_In_Test> question_in_test;

    // Геттеры и сеттеры
    public List<Question_In_Test> getQuestion_in_test() {
        return question_in_test;
    }

    public void setQuestion_in_test(List<Question_In_Test> question_in_test) {
        this.question_in_test = question_in_test;
    }
}