package org.santayn.testing.models.lecture;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.subject.Subject_Lecture;
import org.santayn.testing.models.test.Test_Lecture;

import java.util.List;

@Table
@Data
@Entity
public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer subject_id;
    private Integer test_id;
    private String content;
    private String description;

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Lecture> subject_lecture;

    @OneToOne(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private Test_Lecture test_lecture;
}