package org.santayn.testing.repository;

import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.Question_In_Test;
import org.santayn.testing.models.test.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Integer> {
    @Query("SELECT qit.question FROM Question_In_Test qit WHERE qit.test.id = :test_id")
    List<Question> findQuestionsByTestId(@Param("test_id") Integer test_id);


    // Все тесты по лекции
    @Query("""
        SELECT tl.test
        FROM Test_Lecture tl
        WHERE tl.lecture.id = :lectureId
    """)
    List<Test> findByLecture(@Param("lectureId") Integer lectureId);

    // Тесты по лекции, доступные КОНКРЕТНОЙ группе
    @Query("""
        SELECT DISTINCT t
        FROM Test t
        JOIN t.testGroups tg
        JOIN Test_Lecture tl ON tl.test = t
        WHERE tl.lecture.id = :lectureId
          AND tg.group.id   = :groupId
    """)
    List<Test> findByLectureAndGroup(@Param("lectureId") Integer lectureId,
                                     @Param("groupId") Integer groupId);
}