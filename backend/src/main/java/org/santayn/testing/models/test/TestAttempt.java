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
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.teacher.TeachingAssignmentEnrollment;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`TestAttempts`")
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`Ordinal`", nullable = false)
    private int ordinal = 1;

    @Column(name = "`TestAssignmentId`", nullable = false)
    private Integer testAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TestAssignmentId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private TestAssignment testAssignment;

    @Column(name = "`PersonId`", nullable = false)
    private Integer personId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`PersonId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Person person;

    @Column(name = "`TeachingAssignmentEnrollmentId`")
    private Integer teachingAssignmentEnrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TeachingAssignmentEnrollmentId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private TeachingAssignmentEnrollment teachingAssignmentEnrollment;

    @Column(name = "`Status`", nullable = false)
    private int status = 1;

    @Column(name = "`StartedAt`", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "`CompletedAt`")
    private Instant completedAt;

    @Column(name = "`Score`", precision = 8, scale = 2)
    private BigDecimal score;
}
