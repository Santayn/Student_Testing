package org.santayn.testing.models.question;

import jakarta.persistence.*;
import org.santayn.testing.models.test.Test;


@Table
@Entity
public class Question_Test_Answer {
    @Id
    Integer id;
    Integer id_quest_in_test;
    Integer id_quest_ans;
    Integer id_student;
    @ManyToOne
    @JoinColumn(name = "test_id", referencedColumnName = "id") // Внешний ключ на таблицу Test
    private Test test;

}
