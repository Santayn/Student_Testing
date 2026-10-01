package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.faculty.FacultyMembership;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.repository.FacultyMembershipRepository;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private static final int ROLE_STUDENT = 1;
    private static final int ROLE_GROUP_CURATOR = 2;
    private static final int STATUS_ACTIVE = 1;

    private final PersonRepository personRepository;
    private final FacultyRepository facultyRepository;
    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyMembershipRepository facultyMembershipRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final SubjectMembershipRepository subjectMembershipRepository;

    @Transactional(readOnly = true)
    public List<FacultyMembership> facultyMembers(Integer facultyId, Integer personId, Integer status, boolean activeOnly) {
        return facultyMembershipRepository.findByFilters(facultyId, personId, status, activeOnly);
    }

    @Transactional(readOnly = true)
    public FacultyMembership getFacultyMembership(Integer membershipId) {
        return facultyMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty membership not found: " + membershipId));
    }

    @Transactional
    public FacultyMembership addFacultyMember(Integer facultyId, Integer personId, int role, String notes) {
        requirePerson(personId);
        if (!facultyRepository.existsById(facultyId)) {
            throw new IllegalArgumentException("Faculty not found: " + facultyId);
        }
        requireRole(role);
        if (facultyMembershipRepository.existsByFacultyIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(facultyId, personId, role)) {
            throw new AuthConflictException("Active faculty membership already exists for this faculty, person and role.");
        }

        FacultyMembership membership = new FacultyMembership();
        membership.setFacultyId(facultyId);
        membership.setPersonId(personId);
        membership.setRole(role);
        membership.setStatus(1);
        membership.setAssignedAtUtc(Instant.now());
        membership.setNotes(FacultyService.trimToNull(notes));
        return facultyMembershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<GroupMembership> groupMembers(Integer groupId, Integer personId, Integer status, boolean activeOnly) {
        return groupMembershipRepository.findByFilters(groupId, personId, status, activeOnly);
    }

    @Transactional(readOnly = true)
    public GroupMembership getGroupMembership(Integer membershipId) {
        return groupMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Group membership not found: " + membershipId));
    }

    @Transactional
    public GroupMembership addGroupMember(Integer groupId, Integer personId, int role, String notes) {
        requireRole(role);
        if (role == ROLE_STUDENT) {
            requirePersonForUpdate(personId);
        } else {
            requirePerson(personId);
        }
        if (!groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        if (role == ROLE_STUDENT) {
            requireNoOtherActiveStudentGroup(personId, null);
        }
        if (groupMembershipRepository.existsByGroupIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(groupId, personId, role)) {
            throw new AuthConflictException("Active group membership already exists for this group, person and role.");
        }
        if (role == ROLE_GROUP_CURATOR && groupMembershipRepository.existsByGroupIdAndRoleAndRemovedAtUtcIsNull(groupId, role)) {
            throw new AuthConflictException("Active group curator already exists for this group.");
        }

        GroupMembership membership = new GroupMembership();
        membership.setGroupId(groupId);
        membership.setPersonId(personId);
        membership.setRole(role);
        membership.setStatus(STATUS_ACTIVE);
        membership.setAssignedAtUtc(Instant.now());
        membership.setNotes(FacultyService.trimToNull(notes));
        return groupMembershipRepository.save(membership);
    }

    @Transactional
    public GroupMembership moveStudentToGroup(Integer targetGroupId, Integer personId, String notes) {
        requirePersonForUpdate(personId);
        if (!groupRepository.existsById(targetGroupId)) {
            throw new IllegalArgumentException("Group not found: " + targetGroupId);
        }

        GroupMembership current = groupMembershipRepository
                .findFirstByPersonIdAndRoleAndStatusAndRemovedAtUtcIsNull(personId, ROLE_STUDENT, STATUS_ACTIVE)
                .orElse(null);
        if (current != null && Objects.equals(current.getGroupId(), targetGroupId)) {
            return current;
        }

        if (current != null) {
            current.setStatus(3);
            current.setRemovedAtUtc(Instant.now());
            groupMembershipRepository.saveAndFlush(current);
        }

        GroupMembership target = groupMembershipRepository
                .findFirstByGroupIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(targetGroupId, personId, ROLE_STUDENT)
                .orElse(null);
        if (target != null) {
            target.setStatus(STATUS_ACTIVE);
            target.setRemovedAtUtc(null);
            target.setNotes(FacultyService.trimToNull(notes));
            return groupMembershipRepository.save(target);
        }

        GroupMembership membership = new GroupMembership();
        membership.setGroupId(targetGroupId);
        membership.setPersonId(personId);
        membership.setRole(ROLE_STUDENT);
        membership.setStatus(STATUS_ACTIVE);
        membership.setAssignedAtUtc(Instant.now());
        membership.setNotes(FacultyService.trimToNull(notes));
        return groupMembershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<SubjectMembership> subjectMembers(Integer subjectId, Integer personId, Integer status, boolean activeOnly) {
        return subjectMembershipRepository.findByFilters(subjectId, personId, status, activeOnly);
    }

    @Transactional(readOnly = true)
    public SubjectMembership getSubjectMembership(Integer membershipId) {
        return subjectMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Subject membership not found: " + membershipId));
    }

    @Transactional
    public SubjectMembership addSubjectMember(Integer subjectId, Integer personId, int role, String notes) {
        requirePerson(personId);
        subjectRepository.findByIdForUpdate(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
        requireRole(role);

        SubjectMembership existing = subjectMembershipRepository
                .findFirstBySubjectIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(subjectId, personId, role)
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() == STATUS_ACTIVE) {
                return existing;
            }
            if (existing.getStatus() == 2) {
                existing.setStatus(STATUS_ACTIVE);
                existing.setRemovedAtUtc(null);
                existing.setNotes(FacultyService.trimToNull(notes));
                return subjectMembershipRepository.save(existing);
            }
            throw new AuthConflictException("Subject membership is in an inconsistent non-deleted state.");
        }

        SubjectMembership membership = new SubjectMembership();
        membership.setSubjectId(subjectId);
        membership.setPersonId(personId);
        membership.setRole(role);
        membership.setStatus(STATUS_ACTIVE);
        membership.setAssignedAtUtc(Instant.now());
        membership.setNotes(FacultyService.trimToNull(notes));
        return subjectMembershipRepository.save(membership);
    }

    @Transactional
    public FacultyMembership updateFacultyMembershipStatus(Integer membershipId, int status) {
        FacultyMembership membership = getFacultyMembership(membershipId);
        applyMembershipStatus(membership, status);
        return membership;
    }

    @Transactional
    public FacultyMembership updateFacultyMembership(Integer membershipId, int status, String notes) {
        FacultyMembership membership = getFacultyMembership(membershipId);
        applyMembershipStatus(membership, status);
        membership.setNotes(FacultyService.trimToNull(notes));
        return membership;
    }

    @Transactional
    public GroupMembership updateGroupMembershipStatus(Integer membershipId, int status) {
        GroupMembership membership = getGroupMembership(membershipId);
        if (membership.getRole() == ROLE_STUDENT && status == STATUS_ACTIVE) {
            requirePersonForUpdate(membership.getPersonId());
            requireNoOtherActiveStudentGroup(membership.getPersonId(), membership.getId());
        }
        applyMembershipStatus(membership, status);
        return membership;
    }

    @Transactional
    public GroupMembership updateGroupMembership(Integer membershipId, int status, String notes) {
        GroupMembership membership = getGroupMembership(membershipId);
        if (membership.getRole() == ROLE_STUDENT && status == STATUS_ACTIVE) {
            requirePersonForUpdate(membership.getPersonId());
            requireNoOtherActiveStudentGroup(membership.getPersonId(), membership.getId());
        }
        applyMembershipStatus(membership, status);
        membership.setNotes(FacultyService.trimToNull(notes));
        return membership;
    }

    @Transactional
    public SubjectMembership updateSubjectMembershipStatus(Integer membershipId, int status) {
        SubjectMembership membership = getSubjectMembership(membershipId);
        applyMembershipStatus(membership, status);
        return membership;
    }

    @Transactional
    public SubjectMembership updateSubjectMembership(Integer membershipId, int status, String notes) {
        SubjectMembership membership = getSubjectMembership(membershipId);
        applyMembershipStatus(membership, status);
        membership.setNotes(FacultyService.trimToNull(notes));
        return membership;
    }

    private void requirePerson(Integer personId) {
        if (!personRepository.existsById(personId)) {
            throw new IllegalArgumentException("Person not found: " + personId);
        }
    }

    private void requirePersonForUpdate(Integer personId) {
        personRepository.findByIdForUpdate(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));
    }

    private static void requireRole(int role) {
        if (role < 1 || role > 4) {
            throw new IllegalArgumentException("Role must be between 1 and 4.");
        }
    }

    private static void requireStatus(int status) {
        if (status < 1 || status > 3) {
            throw new IllegalArgumentException("Status must be between 1 and 3.");
        }
    }

    private void requireNoOtherActiveStudentGroup(Integer personId, Integer currentMembershipId) {
        groupMembershipRepository
                .findFirstByPersonIdAndRoleAndStatusAndRemovedAtUtcIsNull(personId, ROLE_STUDENT, STATUS_ACTIVE)
                .filter(existing -> !Objects.equals(existing.getId(), currentMembershipId))
                .ifPresent(existing -> {
                    throw new ActiveStudentGroupConflictException(existing.getGroupId(), existing.getId());
                });
    }

    private static void applyMembershipStatus(FacultyMembership membership, int status) {
        requireStatus(status);
        membership.setStatus(status);
        membership.setRemovedAtUtc(status == 3 ? Instant.now() : null);
    }

    private static void applyMembershipStatus(GroupMembership membership, int status) {
        requireStatus(status);
        membership.setStatus(status);
        membership.setRemovedAtUtc(status == 3 ? Instant.now() : null);
    }

    private static void applyMembershipStatus(SubjectMembership membership, int status) {
        requireStatus(status);
        membership.setStatus(status);
        membership.setRemovedAtUtc(status == 3 ? Instant.now() : null);
    }
}
