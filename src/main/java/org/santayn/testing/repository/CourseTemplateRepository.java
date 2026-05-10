package org.santayn.testing.repository;

import org.santayn.testing.models.course.CourseTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseTemplateRepository extends JpaRepository<CourseTemplate, Integer> {

    List<CourseTemplate> findBySubjectId(Integer subjectId);

    boolean existsBySubjectIdAndName(Integer subjectId, String name);

    boolean existsBySubjectIdAndNameAndIdNot(Integer subjectId, String name, Integer id);

    @Query("""
            select template
            from CourseTemplate template
            where (:subjectId is null or template.subjectId = :subjectId)
              and (:authorPersonId is null or template.authorPersonId = :authorPersonId)
              and (:publicOnly = false or template.publicVisible = true)
            """)
    List<CourseTemplate> findByFilters(@Param("subjectId") Integer subjectId,
                                       @Param("authorPersonId") Integer authorPersonId,
                                       @Param("publicOnly") boolean publicOnly);
}
