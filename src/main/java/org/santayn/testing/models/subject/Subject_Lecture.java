package org.santayn.testing.models.subject;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;

@Entity
@Table(name = "subject_lecture")
@Data
public class Subject_Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "lecture_id")
    private Lecture lecture;
}