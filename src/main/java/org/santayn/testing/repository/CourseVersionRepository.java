package org.santayn.testing.repository;

import org.santayn.testing.models.course.CourseVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseVersionRepository extends JpaRepository<CourseVersion, Integer> {

    List<CourseVersion> findByCourseTemplateIdOrderByVersionNumberDesc(Integer courseTemplateId);

    boolean existsByCourseTemplateIdAndVersionNumber(Integer courseTemplateId, int versionNumber);

    boolean existsByCourseTemplateIdAndVersionNumberAndIdNot(Integer courseTemplateId, int versionNumber, Integer id);

    Optional<CourseVersion> findByCourseTemplateIdAndPublishedTrue(Integer courseTemplateId);

    @Query("""
            select version.courseTemplate.subjectId
            from CourseVersion version
            where version.id = :courseVersionId
            """)
    Optional<Integer> findSubjectIdByCourseVersionId(@Param("courseVersionId") Integer courseVersionId);
}
