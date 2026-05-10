package org.santayn.testing.repository;

import org.santayn.testing.models.teacher.TeachingLoadType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeachingLoadTypeRepository extends JpaRepository<TeachingLoadType, Integer> {

    Optional<TeachingLoadType> findByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);
}
