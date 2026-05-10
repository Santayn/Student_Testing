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

@Service
@RequiredArgsConstructor
public class MembershipService {

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
        requirePerson(personId);
        if (!groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        requireRole(role);
        if (groupMembershipRepository.existsByGroupIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(groupId, personId, role)) {
            throw new AuthConflictException("Active group membership already exists for this group, person and role.");
        }
        if (role == 2 && groupMembershipRepository.existsByGroupIdAndRoleAndRemovedAtUtcIsNull(groupId, role)) {
            throw new AuthConflictException("Active group curator already exists for this group.");
        }

        GroupMembership membership = new GroupMembership();
        membership.setGroupId(groupId);
        membership.setPersonId(personId);
        membership.setRole(role);
        membership.setStatus(1);
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
        if (!subjectRepository.existsById(subjectId)) {
            throw new IllegalArgumentException("Subject not found: " + subjectId);
        }
        requireRole(role);
        if (subjectMembershipRepository.existsBySubjectIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(subjectId, personId, role)) {
            throw new AuthConflictException("Active subject membership already exists for this subject, person and role.");
        }

        SubjectMembership membership = new SubjectMembership();
        membership.setSubjectId(subjectId);
        membership.setPersonId(personId);
        membership.setRole(role);
        membership.setStatus(1);
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
        applyMembershipStatus(membership, status);
        return membership;
    }

    @Transactional
    public GroupMembership updateGroupMembership(Integer membershipId, int status, String notes) {
        GroupMembership membership = getGroupMembership(membershipId);
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
