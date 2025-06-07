package org.santayn.testing.models.answer;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_answer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id",    nullable = false)
    private Integer testId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "correct_answer", length = 4)
    private String correctAnswer;

    @Column(name = "user_answer", length = 50)
    private String userAnswer;

    @Column(name = "is_correct")
    private boolean correct;
}