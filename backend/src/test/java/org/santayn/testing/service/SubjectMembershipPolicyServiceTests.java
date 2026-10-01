package org.santayn.testing.service;

import org.junit.jupiter.api.BeforeEach;
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
class SubjectMembershipPolicyServiceTests {

    @Mock
    private SubjectMembershipRepository subjectMembershipRepository;

    private SubjectMembershipPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new SubjectMembershipPolicyService(subjectMembershipRepository);
    }

    @Test
    void acceptsOnlyActiveTeacherMembership() {
        SubjectMembership membership = membership(1, 1, null);
        when(subjectMembershipRepository.findById(10)).thenReturn(Optional.of(membership));

        assertThat(policyService.requireActiveTeacherSubjectMembership(10)).isSameAs(membership);
        assertThat(policyService.isActiveTeacherSubjectMembership(membership)).isTrue();
    }

    @Test
    void rejectsInactiveTeacherMembership() {
        SubjectMembership membership = membership(1, 2, null);
        when(subjectMembershipRepository.findById(10)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> policyService.requireActiveTeacherSubjectMembership(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void rejectsRemovedTeacherMembership() {
        SubjectMembership membership = membership(1, 1, Instant.parse("2026-09-30T12:00:00Z"));
        when(subjectMembershipRepository.findById(10)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> policyService.requireActiveTeacherSubjectMembership(10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonTeacherMembership() {
        SubjectMembership membership = membership(2, 1, null);
        when(subjectMembershipRepository.findById(10)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> policyService.requireActiveTeacherSubjectMembership(10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SubjectMembership membership(int role, int status, Instant removedAtUtc) {
        SubjectMembership membership = new SubjectMembership();
        membership.setId(10);
        membership.setSubjectId(20);
        membership.setPersonId(30);
        membership.setRole(role);
        membership.setStatus(status);
        membership.setRemovedAtUtc(removedAtUtc);
        return membership;
    }
}
