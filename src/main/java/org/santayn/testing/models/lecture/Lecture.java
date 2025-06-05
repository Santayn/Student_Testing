package org.santayn.testing.models.lecture;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.subject.Subject_Lecture;
import org.santayn.testing.models.test.Test_Lecture;
import org.santayn.testing.models.subject.Subject;

import java.util.List;

@Entity
@Table(name = "lecture")
@Data
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id")
    private Subject subject;

    private String content;
    private String title;
    private String description;

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Lecture> subjectLectureList;

    @OneToOne(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private Test_Lecture testLecture;
}