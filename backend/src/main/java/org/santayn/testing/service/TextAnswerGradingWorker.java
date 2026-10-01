package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.text-answer-grading.worker.enabled", havingValue = "true", matchIfMissing = true)
public class TextAnswerGradingWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextAnswerGradingWorker.class);

    private final TextAnswerGradingQueueService queueService;
    private final TextAnswerEvaluationService evaluationService;

    @Value("${app.text-answer-grading.worker.batch-size:5}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.text-answer-grading.worker.poll-delay-ms:1000}")
    public void processPendingJobs() {
        int limit = Math.max(1, Math.min(batchSize, 100));
        for (int index = 0; index < limit; index++) {
            var task = queueService.claimNext();
            if (task.isEmpty()) {
                return;
            }
            TextAnswerGradingQueueService.GradingTask gradingTask = task.get();
            try {
                TextAnswerEvaluationResult result = evaluationService.evaluate(
                        gradingTask.questionText(),
                        gradingTask.expectedAnswer(),
                        gradingTask.actualAnswer()
                );
                queueService.complete(gradingTask, result);
            } catch (Exception error) {
                LOGGER.warn(
                        "Text answer grading job {} failed: {}",
                        gradingTask.jobId(),
                        error.getMessage()
                );
                queueService.fail(gradingTask, error);
            }
        }
    }
}
