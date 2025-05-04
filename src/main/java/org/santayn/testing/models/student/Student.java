package org.santayn.testing.models.student;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.question.Question_Test_Answer;
import org.santayn.testing.models.subject.Subject_Faculty;
import org.santayn.testing.models.user.User;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String courseCode;
    private String instituteFaculty;

    @OneToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id")
    private User user;

    @OneToOne
    @JoinColumn(name = "id_question_test_answer", referencedColumnName = "id")
    private Question_Test_Answer questionTestAnswer;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group_Student> groupStudents;

    public String getName() {
        if (user == null) {
            return "Unknown";
        }
        return user.getFirstName() + " " + user.getLastName();
    }
}