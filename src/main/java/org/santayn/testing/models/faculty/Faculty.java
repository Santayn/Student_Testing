package org.santayn.testing.models.faculty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.Subject_Faculty;
import org.santayn.testing.models.teacher.Teacher;

import java.util.List;

@Entity
@Table
@Data
public class Faculty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    String name;
    String description;
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Faculty_Teacher> faculty_teacher;
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Subject_Faculty> subject_faculty;
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group> group;
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher> teachers;
}
