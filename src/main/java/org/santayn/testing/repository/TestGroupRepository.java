package org.santayn.testing.repository;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.test.Test_Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestGroupRepository extends JpaRepository<Test_Group, Integer>
{
    @Query("select tg.group from Test_Group tg where tg.test.id = :testId")
    List<Group> findGroupsByTestId(@Param("testId") Integer testId);
}