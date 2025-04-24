package org.santayn.testing.models.question;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table
public class Question {
    @Id
    private Integer id;

    private Integer topic_id;
    private String content;
    private Integer correct_ans_id;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question_In_Test> question_in_test;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question_Answer> question_answer;

    // Геттеры и сеттеры
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTopic_id() {
        return topic_id;
    }

    public void setTopic_id(Integer topic_id) {
        this.topic_id = topic_id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCorrect_ans_id() {
        return correct_ans_id;
    }

    public void setCorrect_ans_id(Integer correct_ans_id) {
        this.correct_ans_id = correct_ans_id;
    }

    public List<Question_In_Test> getQuestion_in_test() {
        return question_in_test;
    }

    public void setQuestion_in_test(List<Question_In_Test> question_in_test) {
        this.question_in_test = question_in_test;
    }

    public List<Question_Answer> getQuestion_answer() {
        return question_answer;
    }

    public void setQuestion_answer(List<Question_Answer> question_answer) {
        this.question_answer = question_answer;
    }
}