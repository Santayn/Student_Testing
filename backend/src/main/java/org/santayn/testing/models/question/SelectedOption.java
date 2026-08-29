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
@Table(name = "`SelectedOptions`")
public class SelectedOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Long id;

    @Column(name = "`QuestionResponseId`", nullable = false)
    private Long questionResponseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`QuestionResponseId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private QuestionResponse questionResponse;

    @Column(name = "`QuestionOptionId`", nullable = false)
    private Long questionOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`QuestionOptionId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private QuestionOption questionOption;
}
