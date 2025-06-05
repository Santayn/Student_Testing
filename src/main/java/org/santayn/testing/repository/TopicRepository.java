package org.santayn.testing.repository;

import org.santayn.testing.models.topic.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Integer> {
    List<Topic> findBySubjectId(Integer subjectId);
}
