package org.santayn.testing.models.lecture;

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

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`StudentLectureProgress`")
public class StudentLectureProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`LectureAssignmentId`", nullable = false)
    private Integer lectureAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`LectureAssignmentId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private LectureAssignment lectureAssignment;

    @Column(name = "`TeachingAssignmentEnrollmentId`", nullable = false)
    private Integer teachingAssignmentEnrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TeachingAssignmentEnrollmentId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private TeachingAssignmentEnrollment teachingAssignmentEnrollment;

    @Column(name = "`Status`", nullable = false)
    private int status = 1;

    @Column(name = "`ProgressPercent`", nullable = false)
    private int progressPercent;

    @Column(name = "`FirstOpenedAtUtc`")
    private Instant firstOpenedAtUtc;

    @Column(name = "`LastVisitedAtUtc`")
    private Instant lastVisitedAtUtc;

    @Column(name = "`CompletedAtUtc`")
    private Instant completedAtUtc;

    @Column(name = "`CompletionSource`")
    private Integer completionSource;

    @Column(name = "`CompletedByPersonId`")
    private Integer completedByPersonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CompletedByPersonId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Person completedByPerson;

    @Column(name = "`LastPositionSeconds`")
    private Long lastPositionSeconds;

    @Column(name = "`TimeSpentSeconds`")
    private Long timeSpentSeconds;

    @Column(name = "`CreatedAtUtc`", nullable = false)
    private Instant createdAtUtc = Instant.now();

    @Column(name = "`UpdatedAtUtc`", nullable = false)
    private Instant updatedAtUtc = Instant.now();
}
