package org.santayn.testing.repository;

import org.santayn.testing.models.question.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByTestQuestionIdOrderByOrdinalAsc(Long testQuestionId);

    boolean existsByTestQuestionIdAndOrdinal(Long testQuestionId, int ordinal);

    boolean existsByTestQuestionIdAndOrdinalAndIdNot(Long testQuestionId, int ordinal, Long id);
}
