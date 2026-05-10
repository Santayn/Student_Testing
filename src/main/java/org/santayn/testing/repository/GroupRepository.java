package org.santayn.testing.repository;

import org.santayn.testing.models.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Integer> {

    Optional<Group> findByCode(String code);

    boolean existsByCode(String code);

    List<Group> findByFacultyId(Integer facultyId);
}
