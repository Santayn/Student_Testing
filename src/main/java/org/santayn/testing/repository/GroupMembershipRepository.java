package org.santayn.testing.repository;

import org.santayn.testing.models.group.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Integer> {

    List<GroupMembership> findByGroupIdAndRemovedAtUtcIsNull(Integer groupId);

    List<GroupMembership> findByPersonIdAndRemovedAtUtcIsNull(Integer personId);

    boolean existsByGroupIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(Integer groupId, Integer personId, int role);

    boolean existsByGroupIdAndRoleAndRemovedAtUtcIsNull(Integer groupId, int role);

    @Query("""
            select membership
            from GroupMembership membership
            where (:groupId is null or membership.groupId = :groupId)
              and (:personId is null or membership.personId = :personId)
              and (:status is null or membership.status = :status)
              and (:activeOnly = false or membership.removedAtUtc is null)
            """)
    List<GroupMembership> findByFilters(@Param("groupId") Integer groupId,
                                        @Param("personId") Integer personId,
                                        @Param("status") Integer status,
                                        @Param("activeOnly") boolean activeOnly);
}
