package org.santayn.testing.repository;

import jakarta.persistence.LockModeType;
import org.santayn.testing.models.question.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select option from QuestionOption option where option.id = :id")
    Optional<QuestionOption> findByIdForUpdate(@Param("id") Long id);

    List<QuestionOption> findByTestQuestionIdOrderByOrdinalAsc(Long testQuestionId);

    List<QuestionOption> findByTestQuestionIdInOrderByTestQuestionIdAscOrdinalAsc(List<Long> testQuestionIds);

    void deleteByTestQuestionIdIn(List<Long> testQuestionIds);

    boolean existsByTestQuestionIdAndOrdinal(Long testQuestionId, int ordinal);

    boolean existsByTestQuestionIdAndOrdinalAndIdNot(Long testQuestionId, int ordinal, Long id);
}
