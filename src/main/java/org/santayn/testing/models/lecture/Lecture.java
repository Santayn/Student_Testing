package org.santayn.testing.models.lecture;

import jakarta.persistence.*;
import org.santayn.testing.models.subject.Subject_Lecture;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.santayn.testing.models.test.Test_Lecture;

import java.util.List;

@Table
@Entity
public class Lecture {
    @Id
    Integer id;
    Integer subject_id;
    Integer test_id;
    String content;
    String description;
    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Lecture> subject_lecture;
    @OneToOne(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private Test_Lecture test_lecture;
}
