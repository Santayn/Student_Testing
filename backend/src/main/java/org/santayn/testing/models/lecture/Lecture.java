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
import org.santayn.testing.models.course.CourseVersion;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.test.Test;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`CourseLectures`")
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`SubjectId`")
    private Integer subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`SubjectId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Subject subject;

    @Column(name = "`SubjectMembershipId`")
    private Integer subjectMembershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`SubjectMembershipId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private SubjectMembership subjectMembership;

    @Column(name = "`CourseVersionId`")
    private Integer courseVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CourseVersionId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private CourseVersion courseVersion;

    @Column(name = "`Ordinal`", nullable = false)
    private int ordinal = 1;

    @Column(name = "`Title`", nullable = false, length = 200)
    private String title;

    @Column(name = "`Description`", length = 2000)
    private String description;

    @Column(name = "`ContentFolderKey`", nullable = false, length = 255)
    private String contentFolderKey;

    @Column(name = "`LinkedTestId`")
    private Integer linkedTestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`LinkedTestId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Test linkedTest;

    @Column(name = "`IsPublic`", nullable = false)
    private boolean publicVisible;
}
