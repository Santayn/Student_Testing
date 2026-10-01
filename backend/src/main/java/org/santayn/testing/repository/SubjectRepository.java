package org.santayn.testing.repository;

import jakarta.persistence.LockModeType;
import org.santayn.testing.models.subject.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {

    Optional<Subject> findByName(String name);

    boolean existsByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select subject from Subject subject where subject.id = :id")
    Optional<Subject> findByIdForUpdate(@Param("id") Integer id);
}
