package org.santayn.testing.models.question;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.answer.Answer;
import org.santayn.testing.models.topic.Topic;

import java.util.List;

@Entity
@Table(name = "question")
@Data
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String text;

    @ManyToOne
    @JoinColumn(name = "topic_id", referencedColumnName = "id")
    private Topic topic;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers;
}