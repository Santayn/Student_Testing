package org.santayn.testing.repository;

import org.santayn.testing.models.teacher.TeachingAssignmentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeachingAssignmentEnrollmentRepository extends JpaRepository<TeachingAssignmentEnrollment, Integer> {

    List<TeachingAssignmentEnrollment> findByTeachingAssignmentId(Integer teachingAssignmentId);

    List<TeachingAssignmentEnrollment> findByGroupMembershipId(Integer groupMembershipId);

    @Query("""
            select enrollment
            from TeachingAssignmentEnrollment enrollment
            where (:teachingAssignmentId is null or enrollment.teachingAssignmentId = :teachingAssignmentId)
              and (:groupMembershipId is null or enrollment.groupMembershipId = :groupMembershipId)
              and (:groupId is null or enrollment.groupId = :groupId)
              and (:status is null or enrollment.status = :status)
            """)
    List<TeachingAssignmentEnrollment> findByFilters(@Param("teachingAssignmentId") Integer teachingAssignmentId,
                                                     @Param("groupMembershipId") Integer groupMembershipId,
                                                     @Param("groupId") Integer groupId,
                                                     @Param("status") Integer status);
}
