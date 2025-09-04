package org.santayn.testing.repository;

import org.santayn.testing.models.test.Test_Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestGroupRepository extends JpaRepository<Test_Group, Integer>
{

}