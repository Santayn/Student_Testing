package org.santayn.testing.models.subject;

import jakarta.persistence.*;
import org.santayn.testing.models.teacher.Teacher_Subject;

import java.util.List;

@Table
@Entity
public class Subject {
    @Id
    Integer id;
    Integer user_id;
    Integer faculty_id;
    String title;
    String description;
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Faculty> subjectFaculties;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Subject> teacherSubjects;


}
