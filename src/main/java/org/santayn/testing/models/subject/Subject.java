package org.santayn.testing.models.subject;

import jakarta.persistence.*;
import org.santayn.testing.models.teacher.Teacher_Subject;

import java.util.List;

@Entity
@Table(name = "subject")
public class Subject {
    @Id
    private Integer id;
    private Integer user_id;
    private Integer faculty_id;
    private String title;
    private String description;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Faculty> subjectFaculties;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teacher_Subject> teacherSubjects;

    // Геттеры и сеттеры
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public Integer getFaculty_id() {
        return faculty_id;
    }

    public void setFaculty_id(Integer faculty_id) {
        this.faculty_id = faculty_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Subject_Faculty> getSubjectFaculties() {
        return subjectFaculties;
    }

    public void setSubjectFaculties(List<Subject_Faculty> subjectFaculties) {
        this.subjectFaculties = subjectFaculties;
    }

    public List<Teacher_Subject> getTeacherSubjects() {
        return teacherSubjects;
    }

    public void setTeacherSubjects(List<Teacher_Subject> teacherSubjects) {
        this.teacherSubjects = teacherSubjects;
    }
}