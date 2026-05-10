package org.santayn.testing.repository;

import org.santayn.testing.models.test.TestAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestAssignmentRepository extends JpaRepository<TestAssignment, Integer> {

    List<TestAssignment> findByTestId(Integer testId);

    List<TestAssignment> findByCourseVersionId(Integer courseVersionId);

    List<TestAssignment> findByCourseLectureId(Integer courseLectureId);

    List<TestAssignment> findByTeachingAssignmentId(Integer teachingAssignmentId);

    Optional<TestAssignment> findFirstByTestIdAndCourseLectureId(Integer testId, Integer courseLectureId);

    @Query("""
            select assignment
            from TestAssignment assignment
            where assignment.testId = :testId
              and assignment.teachingAssignmentId in :teachingAssignmentIds
            """)
    List<TestAssignment> findByTestIdAndTeachingAssignmentIdIn(@Param("testId") Integer testId,
                                                               @Param("teachingAssignmentIds") List<Integer> teachingAssignmentIds);

    boolean existsByTestIdAndScope(Integer testId, int scope);

    boolean existsByTestIdAndCourseVersionId(Integer testId, Integer courseVersionId);

    boolean existsByTestIdAndCourseLectureId(Integer testId, Integer courseLectureId);

    boolean existsByTestIdAndTeachingAssignmentId(Integer testId, Integer teachingAssignmentId);

    boolean existsByTestIdAndScopeAndIdNot(Integer testId, int scope, Integer id);

    boolean existsByTestIdAndCourseVersionIdAndIdNot(Integer testId, Integer courseVersionId, Integer id);

    boolean existsByTestIdAndCourseLectureIdAndIdNot(Integer testId, Integer courseLectureId, Integer id);

    boolean existsByTestIdAndTeachingAssignmentIdAndIdNot(Integer testId, Integer teachingAssignmentId, Integer id);
}
