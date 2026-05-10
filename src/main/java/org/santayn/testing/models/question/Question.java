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
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.topic.Topic;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`TestQuestions`")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Long id;

    @Column(name = "`TestId`")
    private Integer testId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TestId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Test test;

    @Column(name = "`CourseLectureId`")
    private Integer courseLectureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CourseLectureId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Lecture courseLecture;

    @Column(name = "`TopicId`")
    private Integer topicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TopicId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Topic topic;

    @Column(name = "`Type`", nullable = false)
    private int type = 1;

    @Column(name = "`Question`", nullable = false, length = 2000)
    private String question;

    @Column(name = "`Points`", nullable = false, precision = 8, scale = 2)
    private BigDecimal points = BigDecimal.ONE;

    @Column(name = "`Ordinal`", nullable = false)
    private int ordinal = 1;

    @Column(name = "`CorrectAnswer`", length = 2000)
    private String correctAnswer;

    @Column(name = "`IsActive`", nullable = false)
    private boolean active = true;
}
