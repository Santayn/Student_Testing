package org.santayn.testing.models.test;

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
import org.santayn.testing.models.topic.Topic;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`TestQuestionSelectionRules`")
public class TestQuestionSelectionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Long id;

    @Column(name = "`TestId`", nullable = false)
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

    @Column(name = "`QuestionCount`", nullable = false)
    private int questionCount = 1;

    @Column(name = "`TextQuestionCount`", nullable = false)
    private int textQuestionCount;

    @Column(name = "`SingleAnswerQuestionCount`", nullable = false)
    private int singleAnswerQuestionCount;

    @Column(name = "`MultipleAnswerQuestionCount`", nullable = false)
    private int multipleAnswerQuestionCount;

    @Column(name = "`MatchingQuestionCount`", nullable = false)
    private int matchingQuestionCount;

    @Column(name = "`Ordinal`", nullable = false)
    private int ordinal = 1;
}
