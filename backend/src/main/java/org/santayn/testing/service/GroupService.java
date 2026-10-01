package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final FacultyRepository facultyRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;

    @Transactional(readOnly = true)
    public List<Group> findAll(Integer facultyId) {
        return facultyId == null ? groupRepository.findAll() : groupRepository.findByFacultyId(facultyId);
    }

    @Transactional(readOnly = true)
    public Group get(Integer id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));
    }

    @Transactional
    public Group create(String name, String code, Integer facultyId) {
        String normalizedCode = FacultyService.requireText(code, "Code").toLowerCase(Locale.ROOT);
        if (groupRepository.existsByCode(normalizedCode)) {
            throw new AuthConflictException("Group code already exists: " + normalizedCode);
        }
        if (!facultyRepository.existsById(facultyId)) {
            throw new IllegalArgumentException("Faculty not found: " + facultyId);
        }

        Group group = new Group();
        group.setName(FacultyService.requireText(name, "Name"));
        group.setCode(normalizedCode);
        group.setFacultyId(facultyId);
        return groupRepository.save(group);
    }

    @Transactional
    public Group update(Integer id, String name, String code, Integer facultyId) {
        Group group = get(id);
        if (name != null) {
            group.setName(FacultyService.requireText(name, "Name"));
        }
        if (code != null) {
            String normalizedCode = FacultyService.requireText(code, "Code").toLowerCase(Locale.ROOT);
            if (!normalizedCode.equals(group.getCode()) && groupRepository.existsByCode(normalizedCode)) {
                throw new AuthConflictException("Group code already exists: " + normalizedCode);
            }
            group.setCode(normalizedCode);
        }
        if (facultyId != null) {
            if (!facultyRepository.existsById(facultyId)) {
                throw new IllegalArgumentException("Faculty not found: " + facultyId);
            }
            group.setFacultyId(facultyId);
        }
        return group;
    }

    @Transactional
    public void delete(Integer id) {
        Group group = get(id);
        boolean hasMembershipHistory = groupMembershipRepository.existsByGroupId(id);
        boolean hasTeachingAssignments = teachingAssignmentRepository.existsByGroupId(id);
        if (hasMembershipHistory || hasTeachingAssignments) {
            throw new ResourceInUseException(
                    "group",
                    id,
                    "Group cannot be deleted while memberships or teaching assignments reference it.",
                    java.util.Map.of(
                            "groupMemberships", hasMembershipHistory,
                            "teachingAssignments", hasTeachingAssignments
                    )
            );
        }
        groupRepository.delete(group);
    }
}
