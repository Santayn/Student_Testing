package org.santayn.testing.repository;

import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.topic.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

    @Repository
    public interface QuestionRepository extends JpaRepository<Question, Integer> {
        List<Question> findByTopic(Topic topic);
    }

