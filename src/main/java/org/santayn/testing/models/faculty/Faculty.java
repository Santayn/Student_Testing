package org.santayn.testing.models.faculty;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.subject.Subject_Faculty;
import org.santayn.testing.models.teacher.Teacher_Group;

import java.util.List;

@Table
@Entity
@Data
public class Faculty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    Integer teacher_id;
    Integer Subject_id;
    String title;
    String description;
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Faculty_Teacher> faculty_teacher;
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Faculty> subject_faculty;
}
