package org.santayn.testing.models.topic;

import jakarta.persistence.*;
import lombok.Data;
import org.santayn.testing.models.subject.Subject;

@Entity
@Table(name = "topic")
@Data
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "subject_id", referencedColumnName = "id")
    private Subject subject;
}