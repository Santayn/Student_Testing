package org.santayn.testing.models.group;

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

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`GroupMemberships`")
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`GroupId`", nullable = false)
    private Integer groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`GroupId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Group group;

    @Column(name = "`PersonId`", nullable = false)
    private Integer personId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`PersonId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Person person;

    @Column(name = "`Role`", nullable = false)
    private int role;

    @Column(name = "`Status`", nullable = false)
    private int status = 1;

    @Column(name = "`AssignedAtUtc`", nullable = false)
    private Instant assignedAtUtc = Instant.now();

    @Column(name = "`RemovedAtUtc`")
    private Instant removedAtUtc;

    @Column(name = "`Notes`", length = 1000)
    private String notes;
}
