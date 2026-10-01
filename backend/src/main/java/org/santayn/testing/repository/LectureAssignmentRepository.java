package org.santayn.testing.repository;

import jakarta.persistence.LockModeType;
import org.santayn.testing.models.lecture.LectureAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LectureAssignmentRepository extends JpaRepository<LectureAssignment, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from LectureAssignment assignment where assignment.id = :id")
    Optional<LectureAssignment> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByTeachingAssignmentId(Integer teachingAssignmentId);

    List<LectureAssignment> findByTeachingAssignmentId(Integer teachingAssignmentId);

    List<LectureAssignment> findByTeachingAssignmentIdIn(Collection<Integer> teachingAssignmentIds);

    List<LectureAssignment> findByCourseLectureId(Integer courseLectureId);

    boolean existsByCourseLectureId(Integer courseLectureId);

    Optional<LectureAssignment> findByTeachingAssignmentIdAndCourseLectureId(Integer teachingAssignmentId, Integer courseLectureId);
}
