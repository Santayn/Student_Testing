package org.santayn.testing.repository;

import org.santayn.testing.models.test.TestQuestionSelectionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TestQuestionSelectionRuleRepository extends JpaRepository<TestQuestionSelectionRule, Long> {

    List<TestQuestionSelectionRule> findByTestIdOrderByOrdinalAsc(Integer testId);

    void deleteByTestId(Integer testId);

    boolean existsByTestIdAndCourseLectureId(Integer testId, Integer courseLectureId);

    @Query("select count(item) from TestQuestionSelectionRule item where item.courseLectureId = :lectureId or item.topicId in :topicIds")
    long countByLectureOrTopics(@Param("lectureId") Integer lectureId, @Param("topicIds") Collection<Integer> topicIds);
}
