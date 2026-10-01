package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.grading.TextAnswerGradingJob;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TextAnswerGradingJobRepository;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextAnswerGradingQueueLockOrderTests {

    @Mock private TextAnswerGradingJobRepository jobRepository;
    @Mock private QuestionResponseRepository questionResponseRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TestAttemptRepository testAttemptRepository;

    @Test
    void claimLocksAttemptBeforeJob() {
        TextAnswerGradingQueueService service = service();
        var candidate = mock(TextAnswerGradingJobRepository.ClaimableJobCandidate.class);
        when(candidate.getId()).thenReturn(1L);
        when(candidate.getQuestionResponseId()).thenReturn(10L);
        when(jobRepository.findClaimableCandidates(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(questionResponseRepository.findTestAttemptIdById(10L)).thenReturn(Optional.of(20));

        TestAttempt attempt = new TestAttempt();
        attempt.setId(20);
        when(testAttemptRepository.findByIdForUpdate(20)).thenReturn(Optional.of(attempt));

        TextAnswerGradingJob job = processingCandidate(1L, 10L, TextAnswerGradingQueueService.JOB_PENDING, null);
        when(jobRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        QuestionResponse response = new QuestionResponse();
        response.setId(10L);
        response.setTestAttemptId(20);
        response.setTestQuestionId(30L);
        response.setGradingVersion(1);
        response.setAnswerText("answer");
        when(questionResponseRepository.findById(10L)).thenReturn(Optional.of(response));

        Question question = new Question();
        question.setId(30L);
        question.setQuestion("question");
        question.setCorrectAnswer("answer");
        question.setPoints(BigDecimal.ONE);
        when(questionRepository.findById(30L)).thenReturn(Optional.of(question));

        assertThat(service.claimNext()).isPresent();

        InOrder order = inOrder(testAttemptRepository, jobRepository);
        order.verify(testAttemptRepository).findByIdForUpdate(20);
        order.verify(jobRepository).findByIdForUpdate(1L);
    }

    @Test
    void completionLocksAttemptBeforeJob() {
        TextAnswerGradingQueueService service = service();
        TestAttempt attempt = new TestAttempt();
        attempt.setId(20);
        when(testAttemptRepository.findByIdForUpdate(20)).thenReturn(Optional.of(attempt));

        TextAnswerGradingJob job = processingCandidate(
                1L,
                10L,
                TextAnswerGradingQueueService.JOB_PROCESSING,
                "lease-1"
        );
        when(jobRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        QuestionResponse response = new QuestionResponse();
        response.setId(10L);
        response.setTestAttemptId(20);
        response.setGradingVersion(1);
        when(questionResponseRepository.findById(10L)).thenReturn(Optional.of(response));
        when(questionResponseRepository.findByTestAttemptId(20)).thenReturn(List.of(response));

        TextAnswerGradingQueueService.GradingTask task = new TextAnswerGradingQueueService.GradingTask(
                1L,
                "lease-1",
                10L,
                20,
                1,
                "question",
                "answer",
                "answer",
                BigDecimal.ONE
        );
        service.complete(task, TextAnswerEvaluationResult.CORRECT);

        InOrder order = inOrder(testAttemptRepository, jobRepository);
        order.verify(testAttemptRepository).findByIdForUpdate(20);
        order.verify(jobRepository).findByIdForUpdate(1L);
    }

    private TextAnswerGradingQueueService service() {
        return new TextAnswerGradingQueueService(
                jobRepository,
                questionResponseRepository,
                questionRepository,
                testAttemptRepository
        );
    }

    private TextAnswerGradingJob processingCandidate(Long id, Long responseId, String status, String claimToken) {
        TextAnswerGradingJob job = new TextAnswerGradingJob();
        job.setId(id);
        job.setQuestionResponseId(responseId);
        job.setResponseVersion(1);
        job.setStatus(status);
        job.setAttempts(0);
        job.setClaimToken(claimToken);
        job.setCreatedAtUtc(Instant.now());
        job.setUpdatedAtUtc(Instant.now());
        return job;
    }
}
