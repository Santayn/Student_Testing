package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.santayn.testing.models.ai.AiJob;
import org.santayn.testing.models.ai.AiJobStatus;
import org.santayn.testing.repository.AiJobRepository;
import org.santayn.testing.service.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AiJobService {

    private final AiJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public AiJobService(AiJobRepository jobRepository, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    /** Ставит задачу в очередь в текущей транзакции: воркер увидит её после коммита. */
    @Transactional
    public AiJob enqueue(String type, Integer ownerPersonId, Long targetId, Object input) {
        AiJob job = new AiJob();
        job.setType(type);
        job.setOwnerPersonId(ownerPersonId);
        job.setTargetId(targetId);
        job.setInputJson(toJson(input));
        job.setStatus(AiJobStatus.QUEUED);
        // До микросекунд, как хранит база: иначе округление вверх делает задачу «будущей» на доли микросекунды.
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        job.setCreatedAtUtc(now);
        job.setAvailableAtUtc(now);
        return jobRepository.save(job);
    }

    /** Записывает уже выполненный синхронный вызов — для учёта расхода. */
    @Transactional
    public AiJob recordCompleted(String type, Integer ownerPersonId, Long targetId, AiChatClient.AiChatResult usage) {
        AiJob job = new AiJob();
        job.setType(type);
        job.setOwnerPersonId(ownerPersonId);
        job.setTargetId(targetId);
        job.setStatus(AiJobStatus.DONE);
        job.setAttempts(1);
        job.setInputTokens(usage == null ? 0 : usage.inputTokens());
        job.setOutputTokens(usage == null ? 0 : usage.outputTokens());
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        job.setCreatedAtUtc(now);
        job.setAvailableAtUtc(now);
        job.setStartedAtUtc(now);
        job.setFinishedAtUtc(now);
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public AiJob get(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AI job not found: " + id));
    }

    @Transactional(readOnly = true)
    public boolean hasActiveJob(String type, Long targetId) {
        return !jobRepository.findByTypeAndTargetIdAndStatusIn(
                type, targetId, List.of(AiJobStatus.QUEUED, AiJobStatus.RUNNING)
        ).isEmpty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveProgress(Long id, String text, Usage usage) {
        jobRepository.findById(id).ifPresent(job -> {
            job.setProgressText(truncate(text, 300));
            applyUsage(job, usage);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDone(Long id, Object result, Usage usage) {
        jobRepository.findById(id).ifPresent(job -> {
            job.setStatus(AiJobStatus.DONE);
            job.setResultJson(toJson(result));
            job.setProgressText(null);
            job.setError(null);
            job.setFinishedAtUtc(Instant.now());
            applyUsage(job, usage);
        });
    }

    /** @return true, если задача снова поставлена в очередь, false — окончательно провалена. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markFailed(Long id, String message, Usage usage, int maxAttempts) {
        AiJob job = jobRepository.findById(id).orElse(null);
        if (job == null) {
            return false;
        }
        applyUsage(job, usage);
        job.setError(truncate(message, 2000));
        job.setProgressText(null);
        if (job.getAttempts() < maxAttempts) {
            job.setStatus(AiJobStatus.QUEUED);
            job.setAvailableAtUtc(Instant.now().truncatedTo(ChronoUnit.MICROS).plus(Duration.ofSeconds(30L * job.getAttempts())));
            return true;
        }
        job.setStatus(AiJobStatus.FAILED);
        job.setFinishedAtUtc(Instant.now());
        return false;
    }

    public JsonNode readResult(AiJob job) {
        return readJson(job.getResultJson());
    }

    public JsonNode readInput(AiJob job) {
        return readJson(job.getInputJson());
    }

    public <T> T readInput(AiJob job, Class<T> type) {
        try {
            return objectMapper.readValue(job.getInputJson(), type);
        } catch (Exception error) {
            throw new IllegalStateException("AI job input is broken: " + job.getId(), error);
        }
    }

    public AiJobResponse toResponse(AiJob job) {
        return new AiJobResponse(
                job.getId(),
                job.getType(),
                AiJobStatus.name(job.getStatus()),
                job.getProgressText(),
                job.getError(),
                job.getStatus() == AiJobStatus.DONE ? readResult(job) : null,
                job.getCreatedAtUtc(),
                job.getFinishedAtUtc()
        );
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception error) {
            return null;
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException("Failed to serialize AI job data.", error);
        }
    }

    private static void applyUsage(AiJob job, Usage usage) {
        if (usage == null) {
            return;
        }
        job.setInputTokens(job.getInputTokens() + usage.inputTokens());
        job.setOutputTokens(job.getOutputTokens() + usage.outputTokens());
        job.setOcrPages(job.getOcrPages() + usage.ocrPages());
        job.setImagesGenerated(job.getImagesGenerated() + usage.images());
    }

    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    /** Расход, накопленный с прошлого сохранения. */
    public record Usage(long inputTokens, long outputTokens, int ocrPages, int images) {
        public static final Usage NONE = new Usage(0, 0, 0, 0);
    }

    public record AiJobResponse(Long id,
                                String type,
                                String status,
                                String progress,
                                String error,
                                JsonNode result,
                                Instant createdAtUtc,
                                Instant finishedAtUtc) {
    }
}
