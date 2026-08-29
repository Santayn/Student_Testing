package org.santayn.testing.repository;

import org.santayn.testing.models.subject.SubjectMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubjectMembershipRepository extends JpaRepository<SubjectMembership, Integer> {

    List<SubjectMembership> findBySubjectIdAndRemovedAtUtcIsNull(Integer subjectId);

    List<SubjectMembership> findByPersonIdAndRemovedAtUtcIsNull(Integer personId);

    boolean existsBySubjectIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(Integer subjectId, Integer personId, int role);

    @Query("""
            select membership
            from SubjectMembership membership
            where (:subjectId is null or membership.subjectId = :subjectId)
              and (:personId is null or membership.personId = :personId)
              and (:status is null or membership.status = :status)
              and (:activeOnly = false or membership.removedAtUtc is null)
            """)
    List<SubjectMembership> findByFilters(@Param("subjectId") Integer subjectId,
                                          @Param("personId") Integer personId,
                                          @Param("status") Integer status,
                                          @Param("activeOnly") boolean activeOnly);
}
