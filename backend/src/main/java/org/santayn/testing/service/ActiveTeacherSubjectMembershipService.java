package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActiveTeacherSubjectMembershipService {

    public static final int SUBJECT_ROLE_TEACHER = 1;
    public static final int ACTIVE_SUBJECT_MEMBERSHIP_STATUS = 1;

    private final SubjectMembershipRepository subjectMembershipRepository;

    @Transactional(readOnly = true)
    public SubjectMembership requireActiveTeacher(Integer subjectMembershipId) {
        if (subjectMembershipId == null) {
            throw new IllegalArgumentException("Active teacher subject membership is required.");
        }
        SubjectMembership membership = subjectMembershipRepository.findById(subjectMembershipId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subject membership not found: " + subjectMembershipId
                ));
        if (!isActiveTeacher(membership)) {
            throw new IllegalArgumentException(
                    "Active teacher subject membership is required: " + subjectMembershipId
            );
        }
        return membership;
    }

    public boolean isActiveTeacher(SubjectMembership membership) {
        return membership != null
                && membership.getRole() == SUBJECT_ROLE_TEACHER
                && membership.getStatus() == ACTIVE_SUBJECT_MEMBERSHIP_STATUS
                && membership.getRemovedAtUtc() == null;
    }
}
