package org.santayn.testing.models.lecture;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.Test_Lecture;

import java.util.List;

@Data
@Entity
@Table(name = "lecture")
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
    private List<Test_Lecture> testLectures; // Список связей с Test_Lecture

    public void addTestLecture(Test_Lecture testLecture) {
        testLecture.setLecture(this);
        this.testLectures.add(testLecture);
    }

    public void removeTestLecture(Test_Lecture testLecture) {
        this.testLectures.remove(testLecture);
        testLecture.setLecture(null);
    }
    public List<Test> getTests() {
        return testLectures.stream()
                .map(Test_Lecture::getTest)
                .toList();
    }
}