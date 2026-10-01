package org.santayn.testing.repository;

import org.santayn.testing.models.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Integer> {

    Optional<Person> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select person from Person person where person.id = :id")
    Optional<Person> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByEmail(String email);
}
