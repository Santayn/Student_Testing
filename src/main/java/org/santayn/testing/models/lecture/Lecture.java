package org.santayn.testing.models.lecture;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.subject.Subject_Lecture;
import org.santayn.testing.models.test.Test_Lecture;
import org.santayn.testing.models.subject.Subject;

import java.util.List;

@Entity
@Table(name = "lecture") // Можно указать явное имя таблицы
@Data
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Поле subject_id заменено на полноценную JPA-связь
    @ManyToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id")
    private Subject subject;

    // test_id оставим как поле, если Test_Lecture нужен отдельно
    private Integer test_id;

    private String content;
    private String title;
    private String description;

    // Список связей с предметами (если используется join-таблица)
    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject_Lecture> subject_lecture;

    // Одна тестовая лекция (один ко многим?)
    @OneToOne(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private Test_Lecture test_lecture;

}