package org.santayn.testing.repository;

import org.santayn.testing.models.test.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Integer> {

    List<TestAttempt> findByPersonId(Integer personId);

    List<TestAttempt> findByTestAssignmentId(Integer testAssignmentId);

    List<TestAttempt> findByTestAssignmentIdAndPersonId(Integer testAssignmentId, Integer personId);

    long countByTestAssignmentIdAndPersonId(Integer testAssignmentId, Integer personId);

    @Query("""
            select attempt
            from TestAttempt attempt
            where attempt.testAssignment.testId = :testId
              and attempt.personId = :personId
            """)
    List<TestAttempt> findByTestIdAndPersonId(@Param("testId") Integer testId,
                                              @Param("personId") Integer personId);

    @Query("""
            select count(attempt)
            from TestAttempt attempt
            where attempt.testAssignment.testId = :testId
              and attempt.personId = :personId
            """)
    long countByTestIdAndPersonId(@Param("testId") Integer testId,
                                  @Param("personId") Integer personId);
}
