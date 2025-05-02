package org.santayn.testing.models.test;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.question.Question_In_Test;
import org.santayn.testing.models.question.Question_Test_Answer;

import java.util.List;

@Entity
@Table
@Data
public class Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer item_id;
    private Integer question_id;
    private String topic;

    @OneToOne(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private Question_Test_Answer question_test_answer;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question_In_Test> question_in_test;
}