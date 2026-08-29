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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`QuestionOptions`")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Long id;

    @Column(name = "`TestQuestionId`", nullable = false)
    private Long testQuestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TestQuestionId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Question testQuestion;

    @Column(name = "`Text`", nullable = false, length = 2000)
    private String text;

    @Column(name = "`Ordinal`", nullable = false)
    private int ordinal = 1;

    @Column(name = "`IsCorrect`", nullable = false)
    private boolean correct;
}
