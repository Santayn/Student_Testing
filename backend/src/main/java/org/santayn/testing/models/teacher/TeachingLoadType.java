package org.santayn.testing.models.teacher;

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
@Table(name = "`TeachingLoadTypes`")
public class TeachingLoadType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`Name`", nullable = false, length = 100, unique = true, columnDefinition = "citext")
    private String name;

    @Column(name = "`Description`", length = 1000)
    private String description;
}
