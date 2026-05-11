package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.MembershipService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping({"/api/memberships", "/api/v1/memberships"})
public class MembershipRestController {

    private final MembershipService membershipService;

    public MembershipRestController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/faculties")
    public List<ApiResponses.FacultyMembershipResponse> facultyMembers(@RequestParam(required = false) Integer facultyId,
                                                                       @RequestParam(required = false) Integer personId,
                                                                       @RequestParam(required = false) Integer status,
                                                                       @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponses.list(membershipService.facultyMembers(facultyId, personId, status, activeOnly), ApiResponses::facultyMembership);
    }

    @GetMapping("/faculties/{facultyId}")
    public List<ApiResponses.FacultyMembershipResponse> facultyMembersByFaculty(@PathVariable Integer facultyId) {
        return ApiResponses.list(membershipService.facultyMembers(facultyId, null, null, true), ApiResponses::facultyMembership);
    }

    @GetMapping("/faculties/memberships/{membershipId}")
    public ApiResponses.FacultyMembershipResponse facultyMembership(@PathVariable Integer membershipId) {
        return ApiResponses.facultyMembership(membershipService.getFacultyMembership(membershipId));
    }

    @PostMapping("/faculties/{facultyId}")
    public ApiResponses.FacultyMembershipResponse addFacultyMember(@PathVariable Integer facultyId,
                                                                   @Valid @RequestBody MembershipRequest request) {
        return ApiResponses.facultyMembership(membershipService.addFacultyMember(facultyId, request.personId(), request.role(), request.notes()));
    }

    @PutMapping("/faculties/memberships/{membershipId}/status")
    public ApiResponses.FacultyMembershipResponse updateFacultyMembershipStatus(@PathVariable Integer membershipId,
                                                                                @Valid @RequestBody MembershipStatusRequest request) {
        return ApiResponses.facultyMembership(membershipService.updateFacultyMembershipStatus(membershipId, request.status()));
    }

    @PutMapping("/faculties/memberships/{membershipId}")
    public ApiResponses.FacultyMembershipResponse updateFacultyMembership(@PathVariable Integer membershipId,
                                                                          @Valid @RequestBody MembershipUpdateRequest request) {
        return ApiResponses.facultyMembership(membershipService.updateFacultyMembership(membershipId, request.status(), request.notes()));
    }

    @GetMapping("/groups")
    public List<ApiResponses.GroupMembershipResponse> groupMembers(@RequestParam(required = false) Integer groupId,
                                                                   @RequestParam(required = false) Integer personId,
                                                                   @RequestParam(required = false) Integer status,
                                                                   @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponses.list(membershipService.groupMembers(groupId, personId, status, activeOnly), ApiResponses::groupMembership);
    }

    @PostMapping("/groups/{groupId}")
    public ApiResponses.GroupMembershipResponse addGroupMember(@PathVariable Integer groupId,
                                                               @Valid @RequestBody MembershipRequest request) {
        return ApiResponses.groupMembership(membershipService.addGroupMember(groupId, request.personId(), request.role(), request.notes()));
    }

    @GetMapping("/groups/{groupId}")
    public List<ApiResponses.GroupMembershipResponse> groupMembers(@PathVariable Integer groupId) {
        return ApiResponses.list(membershipService.groupMembers(groupId, null, null, true), ApiResponses::groupMembership);
    }

    @GetMapping("/groups/memberships/{membershipId}")
    public ApiResponses.GroupMembershipResponse groupMembership(@PathVariable Integer membershipId) {
        return ApiResponses.groupMembership(membershipService.getGroupMembership(membershipId));
    }

    @PutMapping("/groups/memberships/{membershipId}/status")
    public ApiResponses.GroupMembershipResponse updateGroupMembershipStatus(@PathVariable Integer membershipId,
                                                                            @Valid @RequestBody MembershipStatusRequest request) {
        return ApiResponses.groupMembership(membershipService.updateGroupMembershipStatus(membershipId, request.status()));
    }

    @PutMapping("/groups/memberships/{membershipId}")
    public ApiResponses.GroupMembershipResponse updateGroupMembership(@PathVariable Integer membershipId,
                                                                      @Valid @RequestBody MembershipUpdateRequest request) {
        return ApiResponses.groupMembership(membershipService.updateGroupMembership(membershipId, request.status(), request.notes()));
    }

    @GetMapping("/subjects")
    public List<ApiResponses.SubjectMembershipResponse> subjectMembers(@RequestParam(required = false) Integer subjectId,
                                                                       @RequestParam(required = false) Integer personId,
                                                                       @RequestParam(required = false) Integer status,
                                                                       @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponses.list(membershipService.subjectMembers(subjectId, personId, status, activeOnly), ApiResponses::subjectMembership);
    }

    @GetMapping("/subjects/{subjectId}")
    public List<ApiResponses.SubjectMembershipResponse> subjectMembersBySubject(@PathVariable Integer subjectId) {
        return ApiResponses.list(membershipService.subjectMembers(subjectId, null, null, true), ApiResponses::subjectMembership);
    }

    @GetMapping("/subjects/memberships/{membershipId}")
    public ApiResponses.SubjectMembershipResponse subjectMembership(@PathVariable Integer membershipId) {
        return ApiResponses.subjectMembership(membershipService.getSubjectMembership(membershipId));
    }

    @PostMapping("/subjects/{subjectId}")
    public ApiResponses.SubjectMembershipResponse addSubjectMember(@PathVariable Integer subjectId,
                                                                   @Valid @RequestBody MembershipRequest request) {
        return ApiResponses.subjectMembership(membershipService.addSubjectMember(subjectId, request.personId(), request.role(), request.notes()));
    }

    @PutMapping("/subjects/memberships/{membershipId}/status")
    public ApiResponses.SubjectMembershipResponse updateSubjectMembershipStatus(@PathVariable Integer membershipId,
                                                                                @Valid @RequestBody MembershipStatusRequest request) {
        return ApiResponses.subjectMembership(membershipService.updateSubjectMembershipStatus(membershipId, request.status()));
    }

    @PutMapping("/subjects/memberships/{membershipId}")
    public ApiResponses.SubjectMembershipResponse updateSubjectMembership(@PathVariable Integer membershipId,
                                                                          @Valid @RequestBody MembershipUpdateRequest request) {
        return ApiResponses.subjectMembership(membershipService.updateSubjectMembership(membershipId, request.status(), request.notes()));
    }

    public record MembershipRequest(
            @NotNull Integer personId,
            int role,
            @Size(max = 1000) String notes
    ) {
    }

    public record MembershipStatusRequest(int status) {
    }

    public record MembershipUpdateRequest(
            int status,
            @Size(max = 1000) String notes
    ) {
    }
}
