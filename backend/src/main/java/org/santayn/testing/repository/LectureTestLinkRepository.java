package org.santayn.testing.repository;

import org.santayn.testing.models.lecture.LectureTestLink;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LectureTestLinkRepository extends JpaRepository<LectureTestLink, Integer> {

    @EntityGraph(attributePaths = "test")
    List<LectureTestLink> findByCourseLectureIdOrderByIdAsc(Integer courseLectureId);

    @EntityGraph(attributePaths = "test")
    List<LectureTestLink> findByCourseLectureIdInOrderByCourseLectureIdAscIdAsc(Collection<Integer> courseLectureIds);

    void deleteByTestId(Integer testId);

    boolean existsByCourseLectureIdAndTestId(Integer courseLectureId, Integer testId);
}
