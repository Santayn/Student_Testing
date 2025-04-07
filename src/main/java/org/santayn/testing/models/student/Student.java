package org.santayn.testing.models.student;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.question.Question_Test_Answer;
import org.santayn.testing.models.user.User;

import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class Student {
    @Id
    Integer idStudent;
    Integer courseCode;
    String instituteFaculty;

    @OneToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id") // Внешний ключ на таблицу app_user
    private User user;

    // Связь ManyToOne с Group
    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private Group group;

    // Связь OneToOne с Question_Test_Answer
    @OneToOne
    @JoinColumn(name = "id_question_test_answer", referencedColumnName = "id")
    private Question_Test_Answer questionTestAnswer;
}