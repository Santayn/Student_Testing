package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.MembershipService;
import org.santayn.testing.service.CurrentUserAccessService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/memberships")
public class MembershipRestController {

    private final MembershipService membershipService;
    private final CurrentUserAccessService accessService;

    public MembershipRestController(MembershipService membershipService, CurrentUserAccessService accessService) {
        this.membershipService = membershipService;
        this.accessService = accessService;
    }

    @GetMapping("/faculties")
    public List<ApiResponses.FacultyMembershipResponse> facultyMembers(@RequestParam(required = false) Integer facultyId,
                                                                       @RequestParam(required = false) Integer personId,
                                                                       @RequestParam(required = false) Integer status,
                                                                       @RequestParam(defaultValue = "true") boolean activeOnly,
                                                                       Authentication authentication) {
        return ApiResponses.list(membershipService.facultyMembers(
                facultyId, scopedPersonId(authentication, personId), status, activeOnly
        ), ApiResponses::facultyMembership);
    }

    @GetMapping("/faculties/{facultyId}")
    public List<ApiResponses.FacultyMembershipResponse> facultyMembersByFaculty(@PathVariable Integer facultyId,
                                                                                Authentication authentication) {
        requireStaff(authentication);
        return ApiResponses.list(membershipService.facultyMembers(facultyId, null, null, true), ApiResponses::facultyMembership);
    }

    @GetMapping("/faculties/memberships/{membershipId}")
    public ApiResponses.FacultyMembershipResponse facultyMembership(@PathVariable Integer membershipId,
                                                                    Authentication authentication) {
        var membership = membershipService.getFacultyMembership(membershipId);
        requirePersonOrStaff(authentication, membership.getPersonId());
        return ApiResponses.facultyMembership(membership);
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
                                                                   @RequestParam(defaultValue = "true") boolean activeOnly,
                                                                   Authentication authentication) {
        return ApiResponses.list(membershipService.groupMembers(
                groupId, scopedPersonId(authentication, personId), status, activeOnly
        ), ApiResponses::groupMembership);
    }

    @PostMapping("/groups/{groupId}")
    public ApiResponses.GroupMembershipResponse addGroupMember(@PathVariable Integer groupId,
                                                               @Valid @RequestBody MembershipRequest request) {
        return ApiResponses.groupMembership(membershipService.addGroupMember(groupId, request.personId(), request.role(), request.notes()));
    }

    @GetMapping("/groups/{groupId}")
    public List<ApiResponses.GroupMembershipResponse> groupMembers(@PathVariable Integer groupId,
                                                                   Authentication authentication) {
        requireStaff(authentication);
        return ApiResponses.list(membershipService.groupMembers(groupId, null, null, true), ApiResponses::groupMembership);
    }

    @GetMapping("/groups/memberships/{membershipId}")
    public ApiResponses.GroupMembershipResponse groupMembership(@PathVariable Integer membershipId,
                                                                Authentication authentication) {
        var membership = membershipService.getGroupMembership(membershipId);
        requirePersonOrStaff(authentication, membership.getPersonId());
        return ApiResponses.groupMembership(membership);
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
                                                                       @RequestParam(defaultValue = "true") boolean activeOnly,
                                                                       Authentication authentication) {
        return ApiResponses.list(membershipService.subjectMembers(
                subjectId, scopedPersonId(authentication, personId), status, activeOnly
        ), ApiResponses::subjectMembership);
    }

    @GetMapping("/subjects/{subjectId}")
    public List<ApiResponses.SubjectMembershipResponse> subjectMembersBySubject(@PathVariable Integer subjectId,
                                                                                Authentication authentication) {
        requireStaff(authentication);
        return ApiResponses.list(membershipService.subjectMembers(subjectId, null, null, true), ApiResponses::subjectMembership);
    }

    @GetMapping("/subjects/memberships/{membershipId}")
    public ApiResponses.SubjectMembershipResponse subjectMembership(@PathVariable Integer membershipId,
                                                                    Authentication authentication) {
        var membership = membershipService.getSubjectMembership(membershipId);
        accessService.requireSubjectMembershipRead(authentication, membershipId);
        return ApiResponses.subjectMembership(membership);
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

    private Integer scopedPersonId(Authentication authentication, Integer requestedPersonId) {
        if (accessService.isAdmin(authentication)) {
            return requestedPersonId;
        }
        Integer currentPersonId = accessService.currentPersonId(authentication);
        if (requestedPersonId != null && !currentPersonId.equals(requestedPersonId)) {
            throw new AccessDeniedException("Memberships of another person are not available.");
        }
        return currentPersonId;
    }

    private void requireStaff(Authentication authentication) {
        if (!accessService.isAdmin(authentication)) {
            throw new AccessDeniedException("This membership list is available to administrators only.");
        }
    }

    private void requirePersonOrStaff(Authentication authentication, Integer personId) {
        if (!accessService.isAdmin(authentication)
                && !accessService.currentPersonId(authentication).equals(personId)) {
            throw new AccessDeniedException("Membership belongs to another person.");
        }
    }
}
