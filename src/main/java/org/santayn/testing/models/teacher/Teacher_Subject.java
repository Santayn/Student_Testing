package org.santayn.testing.models.teacher;

import jakarta.persistence.*;
import org.santayn.testing.models.subject.Subject;

@Table
@Entity
public class Teacher_Subject {
    @Id
    Integer id;
    @ManyToOne
    @JoinColumn(name = "teacher_id", referencedColumnName = "id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id")
    private Subject subject;
}
