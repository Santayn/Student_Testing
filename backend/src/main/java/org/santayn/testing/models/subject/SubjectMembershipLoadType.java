package org.santayn.testing.models.subject;

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
import org.santayn.testing.models.teacher.TeachingLoadType;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`SubjectMembershipLoadTypes`")
public class SubjectMembershipLoadType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`SubjectMembershipId`", nullable = false)
    private Integer subjectMembershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`SubjectMembershipId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private SubjectMembership subjectMembership;

    @Column(name = "`TeachingLoadTypeId`", nullable = false)
    private Integer teachingLoadTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`TeachingLoadTypeId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private TeachingLoadType teachingLoadType;

    @Column(name = "`Status`", nullable = false)
    private int status = 1;

    @Column(name = "`AssignedAtUtc`", nullable = false)
    private Instant assignedAtUtc = Instant.now();

    @Column(name = "`RemovedAtUtc`")
    private Instant removedAtUtc;

    @Column(name = "`Notes`", length = 1000)
    private String notes;
}
