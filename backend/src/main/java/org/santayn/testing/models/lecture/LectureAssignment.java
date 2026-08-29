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
import org.santayn.testing.models.teacher.TeachingAssignment;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`LectureAssignments`")
public class LectureAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`TeachingAssignmentId`", nullable = false)
    private Integer teachingAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TeachingAssignmentId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private TeachingAssignment teachingAssignment;

    @Column(name = "`CourseLectureId`", nullable = false)
    private Integer courseLectureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CourseLectureId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Lecture courseLecture;

    @Column(name = "`AvailableFromUtc`")
    private Instant availableFromUtc;

    @Column(name = "`DueToUtc`")
    private Instant dueToUtc;

    @Column(name = "`ClosedAtUtc`")
    private Instant closedAtUtc;

    @Column(name = "`IsRequired`", nullable = false)
    private boolean required = true;

    @Column(name = "`MinProgressPercent`", nullable = false)
    private int minProgressPercent = 100;

    @Column(name = "`Status`", nullable = false)
    private int status = 1;

    @Column(name = "`CreatedAtUtc`", nullable = false)
    private Instant createdAtUtc = Instant.now();

    @Column(name = "`SnapshotCourseVersionId`")
    private Integer snapshotCourseVersionId;

    @Column(name = "`SnapshotGroupId`", nullable = false)
    private Integer snapshotGroupId;

    @Column(name = "`SnapshotTeacherPersonId`", nullable = false)
    private Integer snapshotTeacherPersonId;

    @Column(name = "`SnapshotSemester`", nullable = false)
    private int snapshotSemester;

    @Column(name = "`SnapshotAcademicYear`", nullable = false)
    private int snapshotAcademicYear;
}
