package org.santayn.testing.repository;

import org.santayn.testing.models.test.TestQuestionSelectionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestQuestionSelectionRuleRepository extends JpaRepository<TestQuestionSelectionRule, Long> {

    List<TestQuestionSelectionRule> findByTestIdOrderByOrdinalAsc(Integer testId);

    boolean existsByTestIdAndCourseLectureId(Integer testId, Integer courseLectureId);
}
