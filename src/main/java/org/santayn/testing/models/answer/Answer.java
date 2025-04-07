package org.santayn.testing.models.answer;

import jakarta.persistence.*;
import org.santayn.testing.models.question.Question_Answer;

import java.util.List;

@Table
@Entity

public class
Answer {
    @Id
    Integer id;
    Integer question_id;
    Integer student_id;
    String content;
    @ManyToOne
    @JoinColumn(name = "question_answer_id", referencedColumnName = "id")
    private Question_Answer answer;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question_Answer> question_answer;
}
