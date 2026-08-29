package org.santayn.testing.repository;

import org.santayn.testing.models.subject.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {

    Optional<Subject> findByName(String name);

    boolean existsByName(String name);
}
