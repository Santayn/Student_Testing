package org.santayn.testing.repository;

import jakarta.persistence.LockModeType;
import org.santayn.testing.models.topic.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select topic from Topic topic where topic.id = :id")
    Optional<Topic> findByIdForUpdate(@Param("id") Integer id);

    List<Topic> findAllByOrderBySubjectMembershipIdAscOrdinalAsc();

    List<Topic> findBySubjectIdOrderByOrdinalAsc(Integer subjectId);

    List<Topic> findBySubjectMembershipIdOrderByOrdinalAsc(Integer subjectMembershipId);

    List<Topic> findByCourseLectureIdOrderByOrdinalAsc(Integer courseLectureId);

    boolean existsBySubjectMembershipIdAndOrdinal(Integer subjectMembershipId, int ordinal);

    boolean existsBySubjectMembershipIdAndOrdinalAndIdNot(Integer subjectMembershipId, int ordinal, Integer id);
}
