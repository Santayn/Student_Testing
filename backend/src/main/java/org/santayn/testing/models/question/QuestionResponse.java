package org.santayn.testing.models.question;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.santayn.testing.models.test.TestAttempt;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`QuestionResponses`")
public class QuestionResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Long id;

    @Column(name = "`TestAttemptId`", nullable = false)
    private Integer testAttemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TestAttemptId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private TestAttempt testAttempt;

    @Column(name = "`TestQuestionId`", nullable = false)
    private Long testQuestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TestQuestionId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Question testQuestion;

    @Column(name = "`AnswerText`", length = 4000)
    private String answerText;

    @Column(name = "`IsCorrect`")
    private Boolean correct;

    @Column(name = "`AwardedPoints`", precision = 8, scale = 2)
    private BigDecimal awardedPoints;
}
