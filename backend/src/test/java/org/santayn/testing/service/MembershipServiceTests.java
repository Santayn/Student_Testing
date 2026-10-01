package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(personRepository.findByIdForUpdate(42)).thenReturn(Optional.of(new Person()));
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
        when(personRepository.findByIdForUpdate(42)).thenReturn(Optional.of(new Person()));
        when(groupMembershipRepository.findFirstByPersonIdAndRoleAndStatusAndRemovedAtUtcIsNull(42, 1, 1))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> membershipService.updateGroupMembershipStatus(10, 1))
                .isInstanceOf(AuthConflictException.class)
                .hasMessageContaining("groupId=100");
        assertThat(target.getStatus()).isEqualTo(2);
    }

    @Test
    void studentGroupConflictCarriesCurrentMembershipContext() {
        GroupMembership existing = groupMembership(9, 100, 42, 1, 1);
        when(personRepository.findByIdForUpdate(42)).thenReturn(Optional.of(new Person()));
        when(groupRepository.existsById(200)).thenReturn(true);
        when(groupMembershipRepository.findFirstByPersonIdAndRoleAndStatusAndRemovedAtUtcIsNull(42, 1, 1))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> membershipService.addGroupMember(200, 42, 1, null))
                .isInstanceOfSatisfying(ActiveStudentGroupConflictException.class, ex -> {
                    assertThat(ex.getCurrentGroupId()).isEqualTo(100);
                    assertThat(ex.getCurrentMembershipId()).isEqualTo(9);
                });
    }

    @Test
    void movingStudentClosesOldMembershipAndCreatesTargetMembershipAtomically() {
        GroupMembership current = groupMembership(9, 100, 42, 1, 1);
        when(personRepository.findByIdForUpdate(42)).thenReturn(Optional.of(new Person()));
        when(groupRepository.existsById(200)).thenReturn(true);
        when(groupMembershipRepository.findFirstByPersonIdAndRoleAndStatusAndRemovedAtUtcIsNull(42, 1, 1))
                .thenReturn(Optional.of(current));
        when(groupMembershipRepository.findFirstByGroupIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(200, 42, 1))
                .thenReturn(Optional.empty());
        when(groupMembershipRepository.save(any(GroupMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GroupMembership moved = membershipService.moveStudentToGroup(200, 42, "transfer");

        assertThat(current.getStatus()).isEqualTo(3);
        assertThat(current.getRemovedAtUtc()).isNotNull();
        verify(groupMembershipRepository).saveAndFlush(current);
        assertThat(moved.getGroupId()).isEqualTo(200);
        assertThat(moved.getPersonId()).isEqualTo(42);
        assertThat(moved.getRole()).isEqualTo(1);
        assertThat(moved.getStatus()).isEqualTo(1);
        assertThat(moved.getNotes()).isEqualTo("transfer");
    }

    @Test
    void movingStudentToAlreadyActiveTargetIsIdempotent() {
        GroupMembership current = groupMembership(9, 200, 42, 1, 1);
        when(personRepository.findByIdForUpdate(42)).thenReturn(Optional.of(new Person()));
        when(groupRepository.existsById(200)).thenReturn(true);
        when(groupMembershipRepository.findFirstByPersonIdAndRoleAndStatusAndRemovedAtUtcIsNull(42, 1, 1))
                .thenReturn(Optional.of(current));

        GroupMembership moved = membershipService.moveStudentToGroup(200, 42, "ignored");

        assertThat(moved).isSameAs(current);
        verify(groupMembershipRepository, never()).saveAndFlush(current);
    }

    @Test
    void movingStudentReactivatesSuspendedTargetMembership() {
        GroupMembership current = groupMembership(9, 100, 42, 1, 1);
        GroupMembership suspendedTarget = groupMembership(10, 200, 42, 1, 2);
        when(personRepository.findByIdForUpdate(42)).thenReturn(Optional.of(new Person()));
        when(groupRepository.existsById(200)).thenReturn(true);
        when(groupMembershipRepository.findFirstByPersonIdAndRoleAndStatusAndRemovedAtUtcIsNull(42, 1, 1))
                .thenReturn(Optional.of(current));
        when(groupMembershipRepository.findFirstByGroupIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(200, 42, 1))
                .thenReturn(Optional.of(suspendedTarget));
        when(groupMembershipRepository.save(suspendedTarget)).thenReturn(suspendedTarget);

        GroupMembership moved = membershipService.moveStudentToGroup(200, 42, "restored");

        assertThat(moved).isSameAs(suspendedTarget);
        assertThat(moved.getStatus()).isEqualTo(1);
        assertThat(moved.getRemovedAtUtc()).isNull();
        assertThat(moved.getNotes()).isEqualTo("restored");
    }

    @Test
    void addingExistingActiveSubjectMembershipIsIdempotent() {
        SubjectMembership existing = subjectMembership(15, 300, 42, 1, 1);
        when(personRepository.existsById(42)).thenReturn(true);
        when(subjectRepository.findByIdForUpdate(300)).thenReturn(Optional.of(new Subject()));
        when(subjectMembershipRepository.findFirstBySubjectIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(300, 42, 1))
                .thenReturn(Optional.of(existing));

        SubjectMembership result = membershipService.addSubjectMember(300, 42, 1, "ignored");

        assertThat(result).isSameAs(existing);
        assertThat(result.getStatus()).isEqualTo(1);
        verify(subjectMembershipRepository, never()).save(existing);
    }

    @Test
    void addingSuspendedSubjectMembershipReactivatesExistingRecord() {
        SubjectMembership existing = subjectMembership(16, 300, 42, 1, 2);
        when(personRepository.existsById(42)).thenReturn(true);
        when(subjectRepository.findByIdForUpdate(300)).thenReturn(Optional.of(new Subject()));
        when(subjectMembershipRepository.findFirstBySubjectIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(300, 42, 1))
                .thenReturn(Optional.of(existing));
        when(subjectMembershipRepository.save(existing)).thenReturn(existing);

        SubjectMembership result = membershipService.addSubjectMember(300, 42, 1, "reactivated");

        assertThat(result).isSameAs(existing);
        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getRemovedAtUtc()).isNull();
        assertThat(result.getNotes()).isEqualTo("reactivated");
        verify(subjectMembershipRepository).save(existing);
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

    private static SubjectMembership subjectMembership(int id, int subjectId, int personId, int role, int status) {
        SubjectMembership membership = new SubjectMembership();
        membership.setId(id);
        membership.setSubjectId(subjectId);
        membership.setPersonId(personId);
        membership.setRole(role);
        membership.setStatus(status);
        return membership;
    }
}
