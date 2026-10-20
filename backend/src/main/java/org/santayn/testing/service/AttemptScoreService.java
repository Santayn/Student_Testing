package org.santayn.testing.service;

import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

/** Пересчёт итогового балла попытки после поздней оценки ответа (ИИ или преподавателем). */
@Service
public class AttemptScoreService {

    private static final int ATTEMPT_COMPLETED = 2;

    private final TestAttemptRepository attemptRepository;
    private final QuestionResponseRepository responseRepository;

    public AttemptScoreService(TestAttemptRepository attemptRepository, QuestionResponseRepository responseRepository) {
        this.attemptRepository = attemptRepository;
        this.responseRepository = responseRepository;
    }

    @Transactional
    public void recalculateIfCompleted(Integer attemptId) {
        attemptRepository.findById(attemptId)
                .filter(attempt -> attempt.getStatus() == ATTEMPT_COMPLETED)
                .ifPresent(attempt -> {
                    responseRepository.flush();
                    attempt.setScore(responseRepository.findByTestAttemptId(attemptId)
                            .stream()
                            .map(QuestionResponse::getAwardedPoints)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                });
    }
}
