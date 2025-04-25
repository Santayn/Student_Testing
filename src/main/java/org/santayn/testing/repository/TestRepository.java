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
}