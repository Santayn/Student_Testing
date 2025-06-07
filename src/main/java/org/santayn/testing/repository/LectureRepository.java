package org.santayn.testing.repository;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.topic.Topic;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Integer> {
    @EntityGraph(attributePaths = {"subject"})
    List<Lecture> findLectureBySubjectId(Integer subjectId);
    Optional<Lecture> findBySubjectIdAndId(Integer subjectId, Integer lectureId);

}