package org.santayn.testing.models.course;

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
@Table(name = "`CourseVersions`")
public class CourseVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`CourseTemplateId`", nullable = false)
    private Integer courseTemplateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CourseTemplateId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private CourseTemplate courseTemplate;

    @Column(name = "`VersionNumber`", nullable = false)
    private int versionNumber = 1;

    @Column(name = "`Title`", nullable = false, length = 200)
    private String title;

    @Column(name = "`Description`", length = 2000)
    private String description;

    @Column(name = "`IsPublished`", nullable = false)
    private boolean published;

    @Column(name = "`CreatedByPersonId`", nullable = false)
    private Integer createdByPersonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CreatedByPersonId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Person createdByPerson;

    @Column(name = "`PublishedAtUtc`")
    private Instant publishedAtUtc;

    @Column(name = "`PublishedByPersonId`")
    private Integer publishedByPersonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`PublishedByPersonId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Person publishedByPerson;

    @Column(name = "`ChangeNotes`", length = 2000)
    private String changeNotes;

    @Column(name = "`CreatedAt`", nullable = false)
    private Instant createdAt = Instant.now();
}
