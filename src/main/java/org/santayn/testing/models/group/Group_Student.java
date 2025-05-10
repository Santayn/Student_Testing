package org.santayn.testing.models.group;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.santayn.testing.models.student.Student;

@Entity
@Data
@Table(name = "group_student")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Group_Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include  // Используем только id
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "id")
    private Student student;
}