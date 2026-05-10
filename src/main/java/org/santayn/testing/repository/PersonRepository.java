package org.santayn.testing.repository;

import org.santayn.testing.models.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Integer> {

    Optional<Person> findByEmail(String email);

    boolean existsByEmail(String email);
}
