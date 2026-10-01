package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubjectMembershipPolicyService {

    public static final int ROLE_TEACHER = 1;
    public static final int STATUS_ACTIVE = 1;

    private final SubjectMembershipRepository subjectMembershipRepository;

    public SubjectMembership requireActiveTeacherSubjectMembership(Integer subjectMembershipId) {
        if (subjectMembershipId == null) {
            throw new IllegalArgumentException("Active teacher subject membership is required.");
        }
        SubjectMembership membership = subjectMembershipRepository.findById(subjectMembershipId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subject membership not found: " + subjectMembershipId
                ));
        if (!isActiveTeacherSubjectMembership(membership)) {
            throw new IllegalArgumentException(
                    "Active teacher subject membership is required: " + subjectMembershipId
            );
        }
        return membership;
    }

    public boolean isActiveTeacherSubjectMembership(SubjectMembership membership) {
        return membership != null
                && membership.getRole() == ROLE_TEACHER
                && membership.getStatus() == STATUS_ACTIVE
                && membership.getRemovedAtUtc() == null;
    }
}
