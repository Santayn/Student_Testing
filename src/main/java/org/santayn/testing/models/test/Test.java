package org.santayn.testing.models.test;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.topic.Topic;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "test")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Test_Group> testGroups = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "topic_id", referencedColumnName = "id")
    private Topic topic;

    private String name;
    private int questionCount;
    private String description;
}