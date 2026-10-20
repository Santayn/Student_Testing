package org.santayn.testing.repository;

import org.santayn.testing.models.ai.AiJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {

    @Query("select job.id from AiJob job where job.status = 1 and job.availableAtUtc <= :now order by job.id")
    List<Long> findReadyIds(@Param("now") Instant now, Pageable pageable);

    /** Забрать задачу в работу: срабатывает, только если её ещё никто не взял. */
    @Modifying
    @Query("update AiJob job set job.status = 2, job.startedAtUtc = :now, job.attempts = job.attempts + 1 "
            + "where job.id = :id and job.status = 1")
    int claim(@Param("id") Long id, @Param("now") Instant now);

    /** После перезапуска сервера прерванные задачи возвращаются в очередь. */
    @Modifying
    @Query("update AiJob job set job.status = 1, job.availableAtUtc = :now where job.status = 2")
    int requeueRunning(@Param("now") Instant now);

    @Query("select coalesce(sum(job.inputTokens + job.outputTokens), 0) from AiJob job where job.createdAtUtc >= :since")
    long sumTokensSince(@Param("since") Instant since);

    @Query("select coalesce(sum(job.ocrPages), 0) from AiJob job where job.createdAtUtc >= :since")
    long sumOcrPagesSince(@Param("since") Instant since);

    @Query("select coalesce(sum(job.imagesGenerated), 0) from AiJob job where job.createdAtUtc >= :since")
    long sumImagesSince(@Param("since") Instant since);

    List<AiJob> findByCreatedAtUtcGreaterThanEqualAndCreatedAtUtcLessThan(Instant from, Instant to);

    List<AiJob> findByTypeAndTargetIdAndStatusIn(String type, Long targetId, List<Integer> statuses);

    /** Задачи по удалённой лекции, ещё не взятые в работу, сразу завершаются ошибкой. */
    @Modifying
    @Query("update AiJob job set job.status = 4, job.error = :error, job.progressText = null, job.finishedAtUtc = :now "
            + "where job.status = 1 and job.type in :types and job.targetId = :targetId")
    int failQueuedByTypesAndTargetId(@Param("types") Collection<String> types,
                                     @Param("targetId") Long targetId,
                                     @Param("error") String error,
                                     @Param("now") Instant now);

    @Modifying
    @Query("update AiJob job set job.targetId = null where job.type in :types and job.targetId = :targetId")
    int clearTargetId(@Param("types") Collection<String> types, @Param("targetId") Long targetId);
}
