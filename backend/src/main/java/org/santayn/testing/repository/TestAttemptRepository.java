package org.santayn.testing.repository;

import org.santayn.testing.models.test.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Integer> {

    List<TestAttempt> findByPersonId(Integer personId);

    List<TestAttempt> findByTestAssignmentId(Integer testAssignmentId);

    List<TestAttempt> findByTestAssignmentIdAndPersonId(Integer testAssignmentId, Integer personId);

    long countByTestAssignmentIdAndPersonId(Integer testAssignmentId, Integer personId);
}
