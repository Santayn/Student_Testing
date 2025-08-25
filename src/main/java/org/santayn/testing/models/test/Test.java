package org.santayn.testing.models.test;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.topic.Topic;

@Data
@Entity
@Table(name = "test")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "topic_id", referencedColumnName = "id")
    private Topic topic;

    private String name;
    private int questionCount;
    private String description;
}