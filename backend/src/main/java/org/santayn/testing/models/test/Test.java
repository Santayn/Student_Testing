package org.santayn.testing.models.test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`Tests`")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`Title`", nullable = false, length = 200)
    private String title;

    @Column(name = "`Description`", length = 4000)
    private String description;

    @Column(name = "`Duration`")
    private LocalTime duration;

    @Column(name = "`AttemptsAllowed`", nullable = false)
    private int attemptsAllowed = 1;

    @Column(name = "`QuestionCount`", nullable = false)
    private int questionCount = 1;
}
