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
import org.santayn.testing.models.course.CourseVersion;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.teacher.TeachingAssignment;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`TestAssignments`")
public class TestAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`TestId`", nullable = false)
    private Integer testId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TestId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Test test;

    @Column(name = "`Scope`", nullable = false)
    private int scope = 1;

    @Column(name = "`CourseVersionId`")
    private Integer courseVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CourseVersionId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private CourseVersion courseVersion;

    @Column(name = "`CourseLectureId`")
    private Integer courseLectureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CourseLectureId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Lecture courseLecture;

    @Column(name = "`TeachingAssignmentId`")
    private Integer teachingAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TeachingAssignmentId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private TeachingAssignment teachingAssignment;

    @Column(name = "`AvailableFromUtc`", nullable = false)
    private Instant availableFromUtc;

    @Column(name = "`AvailableUntilUtc`", nullable = false)
    private Instant availableUntilUtc;

    @Column(name = "`Status`", nullable = false)
    private int status = 1;
}
