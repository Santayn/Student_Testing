package org.santayn.testing.models.lecture;

import jakarta.persistence.*;
import org.santayn.testing.models.subject.Subject_Lecture;
import org.santayn.testing.models.test.Test_Lecture;

import java.util.List;

@Table
@Entity
public class Lecture {
    @Id
    private Integer id;
    private Integer subject_id;
    private Integer test_id;
    private String content;
    private String description;

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Lecture> subject_lecture;

    @OneToOne(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private Test_Lecture test_lecture;

    // Геттеры и сеттеры
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubject_id() {
        return subject_id;
    }

    public void setSubject_id(Integer subject_id) {
        this.subject_id = subject_id;
    }

    public Integer getTest_id() {
        return test_id;
    }

    public void setTest_id(Integer test_id) {
        this.test_id = test_id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Subject_Lecture> getSubject_lecture() {
        return subject_lecture;
    }

    public void setSubject_lecture(List<Subject_Lecture> subject_lecture) {
        this.subject_lecture = subject_lecture;
    }

    public Test_Lecture getTest_lecture() {
        return test_lecture;
    }

    public void setTest_lecture(Test_Lecture test_lecture) {
        this.test_lecture = test_lecture;
    }
}