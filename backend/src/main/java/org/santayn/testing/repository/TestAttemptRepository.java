package org.santayn.testing.repository;

import org.santayn.testing.models.test.TestAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from TestAttempt attempt where attempt.id = :id")
    Optional<TestAttempt> findByIdForUpdate(@Param("id") Integer id);

    List<TestAttempt> findByPersonId(Integer personId);

    List<TestAttempt> findByTestAssignmentId(Integer testAssignmentId);

    long countByTestAssignmentId(Integer testAssignmentId);

    long countByTestAssignmentIdIn(Collection<Integer> testAssignmentIds);

    List<TestAttempt> findByTestAssignmentIdIn(List<Integer> testAssignmentIds);

    void deleteByTestAssignmentIdIn(List<Integer> testAssignmentIds);

    List<TestAttempt> findByTestAssignmentIdAndPersonId(Integer testAssignmentId, Integer personId);

    long countByTestAssignmentIdAndPersonId(Integer testAssignmentId, Integer personId);

    long countByTestAssignmentIdAndPersonIdAndStatusNot(Integer testAssignmentId, Integer personId, int status);

    @Query("""
            select case when count(attempt) > 0 then true else false end
            from TestAttempt attempt
            where attempt.testAssignment.testId = :testId
            """)
    boolean existsForTest(@Param("testId") Integer testId);

    @Query("""
            select case when count(attempt) > 0 then true else false end
            from TestAttempt attempt
            where attempt.testAssignment.testId in :testIds
            """)
    boolean existsForAnyTest(@Param("testIds") Collection<Integer> testIds);

    @EntityGraph(attributePaths = {"testAssignment", "person"})
    @Query(
            value = """
                    select attempt
                    from TestAttempt attempt
                    where attempt.status not in :excludedStatuses
                      and (:filterByPerson = false or attempt.personId in :personIds)
                      and (:filterByTest = false or attempt.testAssignment.testId in :testIds)
                    order by case when attempt.completedAt is null then 1 else 0 end,
                             attempt.completedAt desc,
                             attempt.id desc
                    """,
            countQuery = """
                    select count(attempt)
                    from TestAttempt attempt
                    where attempt.status not in :excludedStatuses
                      and (:filterByPerson = false or attempt.personId in :personIds)
                      and (:filterByTest = false or attempt.testAssignment.testId in :testIds)
                    """
    )
    Page<TestAttempt> findResultAttempts(@Param("excludedStatuses") Collection<Integer> excludedStatuses,
                                         @Param("filterByPerson") boolean filterByPerson,
                                         @Param("personIds") Collection<Integer> personIds,
                                         @Param("filterByTest") boolean filterByTest,
                                         @Param("testIds") Collection<Integer> testIds,
                                         Pageable pageable);
}
