package org.santayn.testing.models.topic;

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
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`LectureTopics`")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`SubjectId`", nullable = false)
    private Integer subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`SubjectId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Subject subject;

    @Column(name = "`SubjectMembershipId`")
    private Integer subjectMembershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`SubjectMembershipId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private SubjectMembership subjectMembership;

    @Column(name = "`CourseLectureId`")
    private Integer courseLectureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CourseLectureId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Lecture courseLecture;

    @Column(name = "`Ordinal`", nullable = false)
    private int ordinal = 1;

    @Column(name = "`Name`", nullable = false, length = 200)
    private String name;

    @Column(name = "`Description`", length = 2000)
    private String description;
}
