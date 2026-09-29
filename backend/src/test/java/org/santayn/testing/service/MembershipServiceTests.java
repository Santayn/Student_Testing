package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.repository.FacultyMembershipRepository;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTests {

    @Mock private PersonRepository personRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private FacultyMembershipRepository facultyMembershipRepository;
    @Mock private GroupMembershipRepository groupMembershipRepository;
    @Mock private SubjectMembershipRepository subjectMembershipRepository;

    @InjectMocks
    private MembershipService membershipService;

    @Test
    void addingStudentToSecondActiveGroupIsRejected() {
        GroupMembership existing = groupMembership(9, 100, 42, 1, 1);
        when(personRepository.existsById(42)).thenReturn(true);
        when(groupRepository.existsById(200)).thenReturn(true);
        when(groupMembershipRepository.findFirstByPersonIdAndRoleAndStatusAndRemovedAtUtcIsNull(42, 1, 1))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> membershipService.addGroupMember(200, 42, 1, null))
                .isInstanceOf(AuthConflictException.class)
                .hasMessageContaining("groupId=100");
    }

    @Test
    void reactivatingStudentMembershipIsRejectedWhenAnotherActiveGroupExists() {
        GroupMembership target = groupMembership(10, 200, 42, 1, 2);
        GroupMembership existing = groupMembership(9, 100, 42, 1, 1);
        when(groupMembershipRepository.findById(10)).thenReturn(Optional.of(target));
        when(groupMembershipRepository.findFirstByPersonIdAndRoleAndStatusAndRemovedAtUtcIsNull(42, 1, 1))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> membershipService.updateGroupMembershipStatus(10, 1))
                .isInstanceOf(AuthConflictException.class)
                .hasMessageContaining("groupId=100");
        assertThat(target.getStatus()).isEqualTo(2);
    }

    private static GroupMembership groupMembership(int id, int groupId, int personId, int role, int status) {
        GroupMembership membership = new GroupMembership();
        membership.setId(id);
        membership.setGroupId(groupId);
        membership.setPersonId(personId);
        membership.setRole(role);
        membership.setStatus(status);
        return membership;
    }
}
