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
import org.santayn.testing.models.subject.Subject;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`CourseTemplates`")
public class CourseTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`SubjectId`", nullable = false)
    private Integer subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`SubjectId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Subject subject;

    @Column(name = "`AuthorPersonId`", nullable = false)
    private Integer authorPersonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`AuthorPersonId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Person authorPerson;

    @Column(name = "`Name`", nullable = false, length = 200)
    private String name;

    @Column(name = "`IsPublic`", nullable = false)
    private boolean publicVisible;

    @Column(name = "`CreatedAt`", nullable = false)
    private Instant createdAt = Instant.now();
}
