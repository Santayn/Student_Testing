package org.santayn.testing.repository;

import org.santayn.testing.models.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    @Override
    @EntityGraph(attributePaths = {"person", "roles", "permissions", "roles.permissions"})
    List<User> findAll();

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByPersonId(Integer personId);

    @EntityGraph(attributePaths = {"person", "roles", "permissions", "roles.permissions"})
    Optional<User> findWithSecurityByLogin(String login);

    @EntityGraph(attributePaths = {"person", "roles", "permissions", "roles.permissions"})
    Optional<User> findWithSecurityById(Integer id);

    List<User> findByRoles_Id(Integer roleId);

    @EntityGraph(attributePaths = {"person", "roles"})
    List<User> findByRoles_NameIgnoreCaseAndActiveTrue(String roleName);
}
