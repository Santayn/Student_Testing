package org.santayn.testing.repository;

import org.santayn.testing.models.question.QuestionResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionResponseRepository extends JpaRepository<QuestionResponse, Long> {

    List<QuestionResponse> findByTestAttemptId(Integer testAttemptId);

    List<QuestionResponse> findByTestAttemptIdOrderByIdAsc(Integer testAttemptId);

    Optional<QuestionResponse> findByTestAttemptIdAndTestQuestionId(Integer testAttemptId, Long testQuestionId);
}
