package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TestAttemptExpirationService {

    private final TestAttemptRepository testAttemptRepository;
    private final QuestionResponseRepository questionResponseRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireAttempt(Integer attemptId, Instant expiredAtUtc) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Test attempt not found: " + attemptId));
        if (attempt.getStatus() != TestService.ATTEMPT_STATUS_IN_PROGRESS) {
            return;
        }
        attempt.setStatus(TestService.ATTEMPT_STATUS_EXPIRED);
        attempt.setCompletedAt(expiredAtUtc);
        attempt.setScore(calculateScore(attemptId));
    }

    private BigDecimal calculateScore(Integer testAttemptId) {
        return questionResponseRepository.findByTestAttemptId(testAttemptId)
                .stream()
                .map(QuestionResponse::getAwardedPoints)
                .filter(points -> points != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
