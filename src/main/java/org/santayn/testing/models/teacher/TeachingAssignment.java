package org.santayn.testing.models.teacher;

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
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.SubjectMembership;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`TeachingAssignments`")
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`SubjectMembershipId`", nullable = false)
    private Integer subjectMembershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`SubjectMembershipId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private SubjectMembership subjectMembership;

    @Column(name = "`GroupId`", nullable = false)
    private Integer groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`GroupId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Group group;

    @Column(name = "`LoadTypeId`", nullable = false)
    private Integer loadTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`LoadTypeId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private TeachingLoadType loadType;

    @Column(name = "`CourseVersionId`")
    private Integer courseVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CourseVersionId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private CourseVersion courseVersion;

    @Column(name = "`Semester`", nullable = false)
    private int semester;

    @Column(name = "`StudyCourse`")
    private Integer studyCourse;

    @Column(name = "`AcademicYear`", nullable = false)
    private int academicYear;

    @Column(name = "`HoursPerWeek`", nullable = false, precision = 6, scale = 2)
    private BigDecimal hoursPerWeek = BigDecimal.ZERO;

    @Column(name = "`Status`", nullable = false)
    private int status = 1;

    @Column(name = "`Notes`", length = 1000)
    private String notes;
}
