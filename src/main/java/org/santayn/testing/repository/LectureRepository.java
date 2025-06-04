package org.santayn.testing.repository;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.test.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Integer> {
    @Query(value = "SELECT * FROM Lecture WHERE subject_id = :subject_id", nativeQuery = true)
    List<Lecture> findLectureBySubjectId(@Param("subject_id") Integer subject_id);
    Optional<Lecture> findBySubjectIdAndId(Integer subjectId, Integer lectureId);
}