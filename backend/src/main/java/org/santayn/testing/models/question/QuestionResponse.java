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
import java.time.Instant;

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

    /** Состояние проверки: {@link ResponseReview}. null у старых ответов — то же, что NONE. */
    @Column(name = "`ReviewStatus`")
    private Integer reviewStatus;

    /** Кто поставил баллы: {@link ResponseReview#GRADED_BY_AUTO} и т. д. */
    @Column(name = "`GradedBy`")
    private Integer gradedBy;

    /** Доля баллов по мнению ИИ, 0..1. */
    @Column(name = "`AiScore`", precision = 5, scale = 4)
    private BigDecimal aiScore;

    /** Уверенность ИИ в оценке, 0..1. */
    @Column(name = "`AiConfidence`", precision = 5, scale = 4)
    private BigDecimal aiConfidence;

    /** Комментарий ИИ для студента: чего не хватило в ответе. */
    @Column(name = "`AiFeedback`", length = 2000)
    private String aiFeedback;

    /** Засчитанные и пропущенные критерии: {"matched": [...], "missing": [...], "suspicious": bool}. */
    @Column(name = "`AiDetailsJson`", columnDefinition = "text")
    private String aiDetailsJson;

    @Column(name = "`TeacherComment`", length = 2000)
    private String teacherComment;

    @Column(name = "`GradedByPersonId`")
    private Integer gradedByPersonId;

    @Column(name = "`GradedAtUtc`")
    private Instant gradedAtUtc;

    public int reviewStatusOrNone() {
        return reviewStatus == null ? ResponseReview.NONE : reviewStatus;
    }
}
