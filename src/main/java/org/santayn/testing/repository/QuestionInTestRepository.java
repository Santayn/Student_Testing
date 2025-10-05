// src/main/java/org/santayn/testing/repository/QuestionInTestRepository.java
package org.santayn.testing.repository;

import org.santayn.testing.models.question.Question_In_Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.Collection;

/**
 * Репозиторий для таблицы связей "question_in_test".
 * Здесь нам важно быстро удалять связи по списку question_id.
 */
@Repository
public interface QuestionInTestRepository extends JpaRepository<Question_In_Test, Integer> {

    /**
     * Жёстко удаляет все связи для указанных вопросов.
     * Используем nativeQuery, чтобы не зависеть от структуры сущности.
     *
     * @return количество удалённых строк
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM question_in_test WHERE question_id IN (:questionIds)", nativeQuery = true)
    int deleteByQuestionIds(Collection<Integer> questionIds);

    /**
     * Считает сколько связей существует для заданных вопросов.
     */
    @Query(value = "SELECT COUNT(*) FROM question_in_test WHERE question_id IN (:questionIds)", nativeQuery = true)
    long countByQuestionIds(Collection<Integer> questionIds);
}
