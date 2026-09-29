package org.santayn.testing.repository;

import jakarta.persistence.LockModeType;
import org.santayn.testing.models.role.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    @Override
    @EntityGraph(attributePaths = "permissions")
    List<Role> findAll();

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByName(String name);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.name = :name")
    Optional<Role> findByNameForUpdate(@Param("name") String name);

    boolean existsByName(String name);
}
