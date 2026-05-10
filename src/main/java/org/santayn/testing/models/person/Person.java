package org.santayn.testing.models.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`Person`")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`FirstName`", nullable = false, length = 100)
    private String firstName;

    @Column(name = "`LastName`", nullable = false, length = 100)
    private String lastName;

    @Column(name = "`DateOfBirth`", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "`Email`", nullable = false, length = 255, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "`Phone`", nullable = false, length = 50)
    private String phone = "";
}
