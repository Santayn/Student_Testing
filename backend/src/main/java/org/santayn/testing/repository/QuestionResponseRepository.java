package org.santayn.testing.repository;

import org.santayn.testing.models.question.QuestionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionResponseRepository extends JpaRepository<QuestionResponse, Long> {

    List<QuestionResponse> findByTestAttemptId(Integer testAttemptId);

    List<QuestionResponse> findByTestAttemptIdIn(List<Integer> testAttemptIds);

    List<QuestionResponse> findByTestAttemptIdOrderByIdAsc(Integer testAttemptId);

    Optional<QuestionResponse> findByTestAttemptIdAndTestQuestionId(Integer testAttemptId, Long testQuestionId);

    /** Ответы завершённых попыток в нужных состояниях проверки — для очереди «На проверку». */
    @Query("""
            select response
            from QuestionResponse response
            where response.reviewStatus in :statuses
              and response.testAttempt.status = 2
              and response.testAttempt.testAssignment.testId in :testIds
            order by response.id desc
            """)
    List<QuestionResponse> findForReview(@Param("statuses") Collection<Integer> statuses,
                                         @Param("testIds") Collection<Integer> testIds,
                                         Pageable pageable);

    @Query("""
            select response.reviewStatus, count(response)
            from QuestionResponse response
            where response.reviewStatus in :statuses
              and response.testAttempt.status = 2
              and response.testAttempt.testAssignment.testId in :testIds
            group by response.reviewStatus
            """)
    List<Object[]> countForReview(@Param("statuses") Collection<Integer> statuses,
                                  @Param("testIds") Collection<Integer> testIds);
}
