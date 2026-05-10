package org.santayn.testing.repository;

import org.santayn.testing.models.lecture.LectureTestLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectureTestLinkRepository extends JpaRepository<LectureTestLink, Integer> {

    List<LectureTestLink> findByCourseLectureIdOrderByIdAsc(Integer courseLectureId);

    boolean existsByCourseLectureIdAndTestId(Integer courseLectureId, Integer testId);
}
