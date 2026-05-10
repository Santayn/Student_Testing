package org.santayn.testing.models.faculty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`Faculties`")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`Name`", nullable = false, length = 200)
    private String name;

    @Column(name = "`Code`", nullable = false, length = 50, unique = true, columnDefinition = "citext")
    private String code;

    @Column(name = "`Description`", length = 1000)
    private String description;
}
