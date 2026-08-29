package org.santayn.testing.repository;

import org.santayn.testing.models.lecture.LectureAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LectureAssignmentRepository extends JpaRepository<LectureAssignment, Integer> {

    List<LectureAssignment> findByTeachingAssignmentId(Integer teachingAssignmentId);

    List<LectureAssignment> findByCourseLectureId(Integer courseLectureId);

    Optional<LectureAssignment> findByTeachingAssignmentIdAndCourseLectureId(Integer teachingAssignmentId, Integer courseLectureId);
}
