package org.santayn.testing.models.grading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`TextAnswerGradingJobs`")
public class TextAnswerGradingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Long id;

    @Column(name = "`QuestionResponseId`", nullable = false, unique = true)
    private Long questionResponseId;

    @Column(name = "`ResponseVersion`", nullable = false)
    private int responseVersion;

    @Column(name = "`Status`", nullable = false, length = 20)
    private String status;

    @Column(name = "`Attempts`", nullable = false)
    private int attempts;

    @Column(name = "`NextAttemptAtUtc`")
    private Instant nextAttemptAtUtc;

    @Column(name = "`LastError`", length = 1000)
    private String lastError;

    @Column(name = "`ClaimToken`", length = 36)
    private String claimToken;

    @Column(name = "`CreatedAtUtc`", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "`UpdatedAtUtc`", nullable = false)
    private Instant updatedAtUtc;
}
