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
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.GroupMembership;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`TeachingAssignmentEnrollments`")
public class TeachingAssignmentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`TeachingAssignmentId`", nullable = false)
    private Integer teachingAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TeachingAssignmentId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private TeachingAssignment teachingAssignment;

    @Column(name = "`GroupMembershipId`", nullable = false)
    private Integer groupMembershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`GroupMembershipId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private GroupMembership groupMembership;

    @Column(name = "`GroupId`", nullable = false)
    private Integer groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`GroupId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Group group;

    @Column(name = "`Status`", nullable = false)
    private int status = 1;

    @Column(name = "`EnrolledAtUtc`", nullable = false)
    private Instant enrolledAtUtc = Instant.now();

    @Column(name = "`RemovedAtUtc`")
    private Instant removedAtUtc;

}
