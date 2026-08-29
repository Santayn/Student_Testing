package org.santayn.testing.repository;

import org.santayn.testing.models.question.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTestIdOrderByOrdinalAsc(Integer testId);

    List<Question> findByTopicIdAndTestIdIsNullOrderByOrdinalAsc(Integer topicId);

    List<Question> findByTestIdAndActiveTrueOrderByOrdinalAsc(Integer testId);

    List<Question> findByTopicIdAndTestIdIsNullAndActiveTrueOrderByOrdinalAsc(Integer topicId);

    List<Question> findByTestIdAndCourseLectureIdAndActiveTrueOrderByOrdinalAsc(Integer testId, Integer courseLectureId);

    List<Question> findByTestIdAndCourseLectureIdAndTypeAndActiveTrueOrderByOrdinalAsc(Integer testId, Integer courseLectureId, int type);

    List<Question> findByTestIdAndTopicIdAndActiveTrueOrderByOrdinalAsc(Integer testId, Integer topicId);

    List<Question> findByTestIdAndTopicIdAndTypeAndActiveTrueOrderByOrdinalAsc(Integer testId, Integer topicId, int type);

    List<Question> findByTopicIdAndTestIdIsNullAndTypeAndActiveTrueOrderByOrdinalAsc(Integer topicId, int type);

    boolean existsByTestIdAndOrdinal(Integer testId, int ordinal);

    boolean existsByTestIdAndOrdinalAndIdNot(Integer testId, int ordinal, Long id);

    boolean existsByTopicIdAndTestIdIsNullAndOrdinal(Integer topicId, int ordinal);

    boolean existsByTopicIdAndTestIdIsNullAndOrdinalAndIdNot(Integer topicId, int ordinal, Long id);

    long countByTestId(Integer testId);

    long countByTopicIdAndTestIdIsNull(Integer topicId);

    @Query("select coalesce(max(q.ordinal), 0) from Question q where q.testId = :testId")
    int findMaxOrdinalByTestId(@Param("testId") Integer testId);

    @Query("select coalesce(max(q.ordinal), 0) from Question q where q.topicId = :topicId and q.testId is null")
    int findMaxOrdinalByTopicId(@Param("topicId") Integer topicId);
}
