package org.santayn.testing.repository;

import org.santayn.testing.models.faculty.FacultyMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacultyMembershipRepository extends JpaRepository<FacultyMembership, Integer> {

    List<FacultyMembership> findByFacultyIdAndRemovedAtUtcIsNull(Integer facultyId);

    List<FacultyMembership> findByPersonIdAndRemovedAtUtcIsNull(Integer personId);

    boolean existsByFacultyIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(Integer facultyId, Integer personId, int role);

    @Query("""
            select membership
            from FacultyMembership membership
            where (:facultyId is null or membership.facultyId = :facultyId)
              and (:personId is null or membership.personId = :personId)
              and (:status is null or membership.status = :status)
              and (:activeOnly = false or membership.removedAtUtc is null)
            """)
    List<FacultyMembership> findByFilters(@Param("facultyId") Integer facultyId,
                                          @Param("personId") Integer personId,
                                          @Param("status") Integer status,
                                          @Param("activeOnly") boolean activeOnly);
}
