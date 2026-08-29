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
import org.santayn.testing.models.faculty.Faculty;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`FacultySubjects`")
public class FacultySubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`FacultyId`", nullable = false)
    private Integer facultyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`FacultyId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Faculty faculty;

    @Column(name = "`SubjectId`", nullable = false)
    private Integer subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`SubjectId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Subject subject;
}
