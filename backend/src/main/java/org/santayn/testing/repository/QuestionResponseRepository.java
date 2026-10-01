package org.santayn.testing.repository;

import org.santayn.testing.models.question.QuestionResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionResponseRepository extends JpaRepository<QuestionResponse, Long> {

    List<QuestionResponse> findByTestAttemptId(Integer testAttemptId);

    @EntityGraph(attributePaths = "testQuestion")
    List<QuestionResponse> findByTestAttemptIdIn(List<Integer> testAttemptIds);

    List<QuestionResponse> findByTestAttemptIdOrderByIdAsc(Integer testAttemptId);

    Optional<QuestionResponse> findByTestAttemptIdAndTestQuestionId(Integer testAttemptId, Long testQuestionId);

    @Query("select response.testAttemptId from QuestionResponse response where response.id = :id")
    Optional<Integer> findTestAttemptIdById(@Param("id") Long id);

    boolean existsByTestQuestionId(Long testQuestionId);
}
