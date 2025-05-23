package org.santayn.testing.repository;

import org.santayn.testing.models.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByLogin(String login);

    List<User> findUserByRoleId(Integer roleId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.student LEFT JOIN FETCH u.teacher WHERE u.id = :id")
    Optional<User> findByIdWithRelations(@Param("id") Integer id);
}