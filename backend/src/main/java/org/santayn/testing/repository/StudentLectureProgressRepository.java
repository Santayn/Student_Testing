package org.santayn.testing.repository;

import org.santayn.testing.models.lecture.StudentLectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentLectureProgressRepository extends JpaRepository<StudentLectureProgress, Integer> {

    List<StudentLectureProgress> findByTeachingAssignmentEnrollmentId(Integer teachingAssignmentEnrollmentId);

    List<StudentLectureProgress> findByLectureAssignmentId(Integer lectureAssignmentId);

    Optional<StudentLectureProgress> findByLectureAssignmentIdAndTeachingAssignmentEnrollmentId(
            Integer lectureAssignmentId,
            Integer teachingAssignmentEnrollmentId
    );
}
