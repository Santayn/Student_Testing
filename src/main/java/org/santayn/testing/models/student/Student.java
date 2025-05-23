package org.santayn.testing.models.student;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.question.Question_Test_Answer;
import org.santayn.testing.models.user.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include  // Используем только id для equals/hashCode
    private Integer id;
    @OneToOne
    @JoinColumn(name = "id_group", referencedColumnName = "id")
    private Group group;

    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "id_user", referencedColumnName = "id")
    private User user;

    @OneToOne
    @JoinColumn(name = "id_question_test_answer", referencedColumnName = "id")
    private Question_Test_Answer questionTestAnswer;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group_Student> groupStudents = new ArrayList<>();

    public String getName() {
        if (user == null) {
            return "Unknown";
        }
        return user.getFirstName() + " " + user.getLastName();
    }
}