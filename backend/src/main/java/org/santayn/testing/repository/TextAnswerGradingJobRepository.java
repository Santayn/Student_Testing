package org.santayn.testing.repository;

import jakarta.persistence.LockModeType;
import org.santayn.testing.models.grading.TextAnswerGradingJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TextAnswerGradingJobRepository extends JpaRepository<TextAnswerGradingJob, Long> {

    Optional<TextAnswerGradingJob> findByQuestionResponseId(Long questionResponseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from TextAnswerGradingJob job where job.id = :id")
    Optional<TextAnswerGradingJob> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select job.id as id, job.questionResponseId as questionResponseId
            from TextAnswerGradingJob job
            where (job.status = 'PENDING' and (job.nextAttemptAtUtc is null or job.nextAttemptAtUtc <= :now))
               or (job.status = 'PROCESSING' and job.updatedAtUtc <= :staleBefore)
            order by job.id asc
            """)
    List<ClaimableJobCandidate> findClaimableCandidates(@Param("now") Instant now,
                                                        @Param("staleBefore") Instant staleBefore,
                                                        Pageable pageable);

    interface ClaimableJobCandidate {
        Long getId();

        Long getQuestionResponseId();
    }
}
