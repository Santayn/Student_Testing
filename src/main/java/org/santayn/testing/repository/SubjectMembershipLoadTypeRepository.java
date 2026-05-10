package org.santayn.testing.repository;

import org.santayn.testing.models.subject.SubjectMembershipLoadType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectMembershipLoadTypeRepository extends JpaRepository<SubjectMembershipLoadType, Integer> {

    List<SubjectMembershipLoadType> findBySubjectMembershipIdAndRemovedAtUtcIsNull(Integer subjectMembershipId);

    List<SubjectMembershipLoadType> findByTeachingLoadTypeIdAndRemovedAtUtcIsNull(Integer teachingLoadTypeId);

    Optional<SubjectMembershipLoadType> findBySubjectMembershipIdAndTeachingLoadTypeIdAndRemovedAtUtcIsNull(
            Integer subjectMembershipId,
            Integer teachingLoadTypeId
    );
}
