package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.repository.SubjectMembershipRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveTeacherSubjectMembershipServiceTests {

    @Mock private SubjectMembershipRepository subjectMembershipRepository;

    @Test
    void returnsOnlyActiveNonRemovedTeacherMembership() {
        SubjectMembership membership = membership(7, 1, 1, null);
        when(subjectMembershipRepository.findById(7)).thenReturn(Optional.of(membership));
        ActiveTeacherSubjectMembershipService service = new ActiveTeacherSubjectMembershipService(
                subjectMembershipRepository
        );

        assertThat(service.requireActiveTeacher(7)).isSameAs(membership);
    }

    @Test
    void rejectsInactiveRemovedOrNonTeacherMemberships() {
        ActiveTeacherSubjectMembershipService service = new ActiveTeacherSubjectMembershipService(
                subjectMembershipRepository
        );
        SubjectMembership inactive = membership(7, 1, 2, null);
        SubjectMembership removed = membership(8, 1, 1, Instant.now());
        SubjectMembership student = membership(9, 2, 1, null);
        when(subjectMembershipRepository.findById(7)).thenReturn(Optional.of(inactive));
        when(subjectMembershipRepository.findById(8)).thenReturn(Optional.of(removed));
        when(subjectMembershipRepository.findById(9)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.requireActiveTeacher(7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
        assertThatThrownBy(() -> service.requireActiveTeacher(8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
        assertThatThrownBy(() -> service.requireActiveTeacher(9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    private static SubjectMembership membership(int id, int role, int status, Instant removedAtUtc) {
        SubjectMembership membership = new SubjectMembership();
        membership.setId(id);
        membership.setRole(role);
        membership.setStatus(status);
        membership.setRemovedAtUtc(removedAtUtc);
        return membership;
    }
}
