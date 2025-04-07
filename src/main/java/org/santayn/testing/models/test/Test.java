package org.santayn.testing.models.test;

import jakarta.persistence.*;
import org.santayn.testing.models.question.Question_In_Test;
import org.santayn.testing.models.question.Question_Test_Answer;
import org.santayn.testing.models.subject.Subject_Lecture;

import java.util.List;

@Table
@Entity 
public class Test {
    @Id
    Integer id;
    Integer item_id;
    Integer question_id;
    String topic;
    @OneToOne(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private Test_Lecture test_lecture;
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question_In_Test> question_in_test;
    @OneToOne(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private Question_Test_Answer question_test_answer;
}
