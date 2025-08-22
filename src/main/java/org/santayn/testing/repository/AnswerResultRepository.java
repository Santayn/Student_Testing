package org.santayn.testing.repository;

import org.santayn.testing.models.answer.AnswerResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnswerResultRepository extends JpaRepository<AnswerResult, Long> {
    List<AnswerResult> findByTestId(Integer testId);
    List<AnswerResult> findByStudentId(Integer studentId);
}
