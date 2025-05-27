package org.santayn.testing.models.subject;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.teacher.Teacher_Subject;

import java.util.List;

@Entity
@Data
@Table(name = "subject")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String description;
    private String teacher_id;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Faculty> subjectFaculties;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Subject> teacherSubjects;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Lecture> subjectlecture;

}