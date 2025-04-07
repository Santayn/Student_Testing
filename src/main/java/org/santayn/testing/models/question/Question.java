package org.santayn.testing.models.question;

import jakarta.persistence.*;
import org.santayn.testing.models.test.Test_Lecture;

import java.util.List;

@Table
@Entity
public class Question {
    @Id
    Integer id;
    Integer topic_id;
    String content;
    Integer correct_ans_id;
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question_In_Test> question_in_test;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question_Answer> question_answer;
}
