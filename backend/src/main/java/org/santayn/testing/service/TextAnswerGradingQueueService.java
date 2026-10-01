package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.grading.TextAnswerGradingJob;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TextAnswerGradingJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TextAnswerGradingQueueService {

    public static final String JOB_PENDING = "PENDING";
    public static final String JOB_PROCESSING = "PROCESSING";
    public static final String JOB_COMPLETED = "COMPLETED";
    public static final String JOB_FAILED = "FAILED";

    public static final String GRADING_PENDING = "PENDING";
    public static final String GRADING_GRADED = "GRADED";
    public static final String GRADING_FAILED = "FAILED";

    private final TextAnswerGradingJobRepository jobRepository;
    private final QuestionResponseRepository questionResponseRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository testAttemptRepository;

    @Value("${app.text-answer-grading.worker.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.text-answer-grading.worker.retry-delay-seconds:5}")
    private long retryDelaySeconds;

    @Value("${app.text-answer-grading.worker.claim-timeout-seconds:60}")
    private long claimTimeoutSeconds;

    @Transactional
    public void enqueue(QuestionResponse response) {
        Instant now = Instant.now();
        TextAnswerGradingJob job = jobRepository.findByQuestionResponseId(response.getId())
                .orElseGet(TextAnswerGradingJob::new);
        if (job.getId() == null) {
            job.setQuestionResponseId(response.getId());
            job.setCreatedAtUtc(now);
        }
        job.setResponseVersion(response.getGradingVersion());
        job.setStatus(JOB_PENDING);
        job.setAttempts(0);
        job.setNextAttemptAtUtc(now);
        job.setLastError(null);
        job.setClaimToken(null);
        job.setUpdatedAtUtc(now);
        jobRepository.save(job);
        refreshAttemptState(response.getTestAttemptId(), now);
    }

    @Transactional
    public Optional<GradingTask> claimNext() {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(Math.max(5, claimTimeoutSeconds), ChronoUnit.SECONDS);
        List<TextAnswerGradingJobRepository.ClaimableJobCandidate> claimable = jobRepository.findClaimableCandidates(
                now,
                staleBefore,
                PageRequest.of(0, 1)
        );
        if (claimable.isEmpty()) {
            return Optional.empty();
        }

        TextAnswerGradingJobRepository.ClaimableJobCandidate candidate = claimable.get(0);
        Integer testAttemptId = questionResponseRepository.findTestAttemptIdById(candidate.getQuestionResponseId())
                .orElse(null);
        if (testAttemptId == null) {
            TextAnswerGradingJob orphan = jobRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (isClaimable(orphan, now, staleBefore)) {
                markOrphanFailed(orphan, "Question response no longer exists.", now);
            }
            return Optional.empty();
        }

        TestAttempt attempt = testAttemptRepository.findByIdForUpdate(testAttemptId).orElse(null);
        TextAnswerGradingJob job = jobRepository.findByIdForUpdate(candidate.getId()).orElse(null);
        if (attempt == null) {
            if (isClaimable(job, now, staleBefore)) {
                markOrphanFailed(job, "Test attempt no longer exists.", now);
            }
            return Optional.empty();
        }
        if (!isClaimable(job, now, staleBefore)) {
            return Optional.empty();
        }

        QuestionResponse response = questionResponseRepository.findById(job.getQuestionResponseId()).orElse(null);
        if (response == null || !Objects.equals(response.getTestAttemptId(), attempt.getId())) {
            markOrphanFailed(job, "Question response no longer exists for the locked attempt.", now);
            return Optional.empty();
        }
        Question question = questionRepository.findById(response.getTestQuestionId()).orElse(null);
        if (question == null) {
            markOrphanFailed(job, "Question no longer exists.", now);
            response.setGradingStatus(GRADING_FAILED);
            response.setGradingError(job.getLastError());
            response.setGradingUpdatedAtUtc(now);
            refreshAttemptState(attempt, now);
            return Optional.empty();
        }

        String claimToken = UUID.randomUUID().toString();
        job.setStatus(JOB_PROCESSING);
        job.setAttempts(job.getAttempts() + 1);
        job.setClaimToken(claimToken);
        job.setUpdatedAtUtc(now);

        return Optional.of(new GradingTask(
                job.getId(),
                claimToken,
                response.getId(),
                response.getTestAttemptId(),
                job.getResponseVersion(),
                question.getQuestion(),
                question.getCorrectAnswer(),
                response.getAnswerText(),
                question.getPoints()
        ));
    }

    @Transactional
    public void complete(GradingTask task, TextAnswerEvaluationResult evaluation) {
        Instant now = Instant.now();
        TestAttempt attempt = testAttemptRepository.findByIdForUpdate(task.testAttemptId()).orElse(null);
        TextAnswerGradingJob job = jobRepository.findByIdForUpdate(task.jobId()).orElse(null);
        if (attempt == null) {
            if (matchesClaim(job, task)) {
                markOrphanFailed(job, "Test attempt no longer exists.", now);
            }
            return;
        }
        if (!matchesClaim(job, task)) {
            return;
        }
        QuestionResponse response = questionResponseRepository.findById(task.questionResponseId()).orElse(null);
        if (response == null || response.getGradingVersion() != task.responseVersion()) {
            return;
        }

        response.setCorrect(evaluation.correct());
        BigDecimal points = task.questionPoints() == null ? BigDecimal.ZERO : task.questionPoints();
        response.setAwardedPoints(points.multiply(evaluation.scoreRatio()).setScale(2, RoundingMode.HALF_UP));
        response.setGradingStatus(GRADING_GRADED);
        response.setGradingError(null);
        response.setGradingUpdatedAtUtc(now);

        job.setStatus(JOB_COMPLETED);
        job.setNextAttemptAtUtc(null);
        job.setLastError(null);
        job.setClaimToken(null);
        job.setUpdatedAtUtc(now);
        refreshAttemptState(attempt, now);
    }

    @Transactional
    public void fail(GradingTask task, Throwable error) {
        Instant now = Instant.now();
        TestAttempt attempt = testAttemptRepository.findByIdForUpdate(task.testAttemptId()).orElse(null);
        TextAnswerGradingJob job = jobRepository.findByIdForUpdate(task.jobId()).orElse(null);
        if (attempt == null) {
            if (matchesClaim(job, task)) {
                markOrphanFailed(job, "Test attempt no longer exists.", now);
            }
            return;
        }
        if (!matchesClaim(job, task)) {
            return;
        }
        String message = truncateError(error);
        job.setLastError(message);
        job.setUpdatedAtUtc(now);

        QuestionResponse response = questionResponseRepository.findById(task.questionResponseId()).orElse(null);
        if (job.getAttempts() >= Math.max(1, maxAttempts)) {
            job.setStatus(JOB_FAILED);
            job.setNextAttemptAtUtc(null);
            job.setClaimToken(null);
            if (response != null && response.getGradingVersion() == task.responseVersion()) {
                response.setCorrect(null);
                response.setAwardedPoints(null);
                response.setGradingStatus(GRADING_FAILED);
                response.setGradingError(message);
                response.setGradingUpdatedAtUtc(now);
            }
        } else {
            job.setStatus(JOB_PENDING);
            job.setClaimToken(null);
            job.setNextAttemptAtUtc(now.plus(
                    Math.max(1, retryDelaySeconds) * Math.max(1, job.getAttempts()),
                    ChronoUnit.SECONDS
            ));
            if (response != null && response.getGradingVersion() == task.responseVersion()) {
                response.setGradingStatus(GRADING_PENDING);
                response.setGradingError(message);
                response.setGradingUpdatedAtUtc(now);
            }
        }
        refreshAttemptState(attempt, now);
    }

    @Transactional
    public void refreshAttemptState(Integer testAttemptId) {
        refreshAttemptState(testAttemptId, Instant.now());
    }

    private void refreshAttemptState(Integer testAttemptId, Instant now) {
        TestAttempt attempt = testAttemptRepository.findByIdForUpdate(testAttemptId).orElse(null);
        if (attempt == null) {
            return;
        }
        refreshAttemptState(attempt, now);
    }

    private void refreshAttemptState(TestAttempt attempt, Instant now) {
        List<QuestionResponse> responses = questionResponseRepository.findByTestAttemptId(attempt.getId());
        boolean hasFailed = responses.stream().anyMatch(item -> GRADING_FAILED.equals(item.getGradingStatus()));
        boolean hasPending = responses.stream().anyMatch(item -> GRADING_PENDING.equals(item.getGradingStatus()));
        attempt.setGradingStatus(hasFailed ? GRADING_FAILED : hasPending ? GRADING_PENDING : GRADING_GRADED);
        attempt.setGradingUpdatedAtUtc(now);
        attempt.setScore(responses.stream()
                .map(QuestionResponse::getAwardedPoints)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private boolean isClaimable(TextAnswerGradingJob job, Instant now, Instant staleBefore) {
        if (job == null) {
            return false;
        }
        if (JOB_PENDING.equals(job.getStatus())) {
            return job.getNextAttemptAtUtc() == null || !job.getNextAttemptAtUtc().isAfter(now);
        }
        return JOB_PROCESSING.equals(job.getStatus())
                && job.getUpdatedAtUtc() != null
                && !job.getUpdatedAtUtc().isAfter(staleBefore);
    }

    private boolean matchesClaim(TextAnswerGradingJob job, GradingTask task) {
        return job != null
                && JOB_PROCESSING.equals(job.getStatus())
                && job.getResponseVersion() == task.responseVersion()
                && Objects.equals(job.getClaimToken(), task.claimToken());
    }

    private void markOrphanFailed(TextAnswerGradingJob job, String message, Instant now) {
        job.setStatus(JOB_FAILED);
        job.setLastError(message);
        job.setClaimToken(null);
        job.setNextAttemptAtUtc(null);
        job.setUpdatedAtUtc(now);
    }

    private static String truncateError(Throwable error) {
        String message = error == null ? "Text answer grading failed." : FacultyService.trimToNull(error.getMessage());
        if (message == null) {
            message = error == null ? "Text answer grading failed." : error.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    public record GradingTask(Long jobId,
                              String claimToken,
                              Long questionResponseId,
                              Integer testAttemptId,
                              int responseVersion,
                              String questionText,
                              String expectedAnswer,
                              String actualAnswer,
                              BigDecimal questionPoints) {
    }
}
