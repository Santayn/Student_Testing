package org.santayn.testing.models.teacher;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.subject.Subject;

@Table
@Entity
@Data
public class Teacher_Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @ManyToOne
    @JoinColumn(name = "teacher_id", referencedColumnName = "id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id")
    private Subject subject;
}
