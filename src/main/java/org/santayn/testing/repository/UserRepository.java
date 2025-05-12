package org.santayn.testing.repository;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByLogin(String login);
    @Query("SELECT u FROM User u WHERE u.role.id = :roleId")
    List<User> findUserByRoleId(@Param("roleId") Integer roleId);
}