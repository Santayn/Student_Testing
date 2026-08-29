package org.santayn.testing.repository;

import org.santayn.testing.models.lecture.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LectureRepository extends JpaRepository<Lecture, Integer> {

    List<Lecture> findBySubjectIdOrderByOrdinalAsc(Integer subjectId);

    List<Lecture> findBySubjectMembershipIdOrderByOrdinalAsc(Integer subjectMembershipId);

    List<Lecture> findByCourseVersionIdOrderByOrdinalAsc(Integer courseVersionId);

    boolean existsBySubjectMembershipIdAndOrdinal(Integer subjectMembershipId, int ordinal);

    boolean existsBySubjectMembershipIdAndOrdinalAndIdNot(Integer subjectMembershipId, int ordinal, Integer id);

    boolean existsByCourseVersionIdAndOrdinal(Integer courseVersionId, int ordinal);

    boolean existsByCourseVersionIdAndOrdinalAndIdNot(Integer courseVersionId, int ordinal, Integer id);

    @Query("""
            select coalesce(lecture.subjectId, lecture.courseVersion.courseTemplate.subjectId)
            from Lecture lecture
            where lecture.id = :courseLectureId
            """)
    Optional<Integer> findSubjectIdByLectureId(@Param("courseLectureId") Integer courseLectureId);
}
