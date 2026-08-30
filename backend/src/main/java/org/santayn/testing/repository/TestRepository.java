package org.santayn.testing.repository;

import org.santayn.testing.models.test.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select test from Test test where test.id = :id")
    Optional<Test> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
            select distinct test
            from Test test
            join TestQuestionSelectionRule rule
              on rule.testId = test.id
            left join Topic topic
              on topic.id = rule.topicId
            left join Lecture lecture
              on lecture.id = rule.courseLectureId
            left join lecture.courseVersion version
            left join version.courseTemplate template
            where coalesce(topic.subjectId, lecture.subjectId, template.subjectId) = :subjectId
            """)
    List<Test> findBySubjectId(@Param("subjectId") Integer subjectId);

    @Query("""
            select case when count(test) > 0 then true else false end
            from Test test
            join TestQuestionSelectionRule rule
              on rule.testId = test.id
            left join Topic topic
              on topic.id = rule.topicId
            left join Lecture lecture
              on lecture.id = rule.courseLectureId
            left join lecture.courseVersion version
            left join version.courseTemplate template
            where test.id = :testId
              and coalesce(topic.subjectId, lecture.subjectId, template.subjectId) = :subjectId
            """)
    boolean existsInSubject(@Param("testId") Integer testId, @Param("subjectId") Integer subjectId);
}
