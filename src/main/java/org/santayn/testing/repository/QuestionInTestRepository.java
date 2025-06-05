package org.santayn.testing.repository;

import org.santayn.testing.models.question.Question_In_Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Добавь эту аннотацию
public interface QuestionInTestRepository extends JpaRepository<Question_In_Test, Integer> {
    List<Question_In_Test> findByTestId(Integer testId);
}