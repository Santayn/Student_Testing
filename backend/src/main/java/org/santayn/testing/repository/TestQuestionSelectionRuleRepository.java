package org.santayn.testing.repository;

import org.santayn.testing.models.test.TestQuestionSelectionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestQuestionSelectionRuleRepository extends JpaRepository<TestQuestionSelectionRule, Long> {

    List<TestQuestionSelectionRule> findByTestIdOrderByOrdinalAsc(Integer testId);

    @Query("select distinct rule.testId from TestQuestionSelectionRule rule where rule.topicId = :topicId order by rule.testId")
    List<Integer> findDistinctTestIdsByTopicId(@Param("topicId") Integer topicId);

    void deleteByTestId(Integer testId);

    boolean existsByCourseLectureId(Integer courseLectureId);

    boolean existsByTopicId(Integer topicId);

    boolean existsByTestIdAndCourseLectureId(Integer testId, Integer courseLectureId);
}
