package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.TeachingService;
import org.santayn.testing.service.CurrentUserAccessService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teaching")
public class TeachingRestController {

    private final TeachingService teachingService;
    private final CurrentUserAccessService accessService;

    public TeachingRestController(TeachingService teachingService, CurrentUserAccessService accessService) {
        this.teachingService = teachingService;
        this.accessService = accessService;
    }

    @GetMapping("/load-types")
    public List<ApiResponses.TeachingLoadTypeResponse> loadTypes() {
        return ApiResponses.list(teachingService.findLoadTypes(), ApiResponses::teachingLoadType);
    }

    @GetMapping("/load-types/{loadTypeId}")
    public ApiResponses.TeachingLoadTypeResponse loadType(@PathVariable Integer loadTypeId) {
        return ApiResponses.teachingLoadType(teachingService.getLoadType(loadTypeId));
    }

    @PostMapping("/load-types")
    public ApiResponses.TeachingLoadTypeResponse createLoadType(@Valid @RequestBody LoadTypeRequest request) {
        return ApiResponses.teachingLoadType(teachingService.createLoadType(request.name(), request.description()));
    }

    @PutMapping("/load-types/{loadTypeId}")
    public ApiResponses.TeachingLoadTypeResponse updateLoadType(@PathVariable Integer loadTypeId,
                                                                @Valid @RequestBody LoadTypeRequest request) {
        return ApiResponses.teachingLoadType(teachingService.updateLoadType(loadTypeId, request.name(), request.description()));
    }

    @GetMapping("/subject-load-types")
    public List<ApiResponses.SubjectMembershipLoadTypeResponse> subjectLoadTypes(
            @RequestParam(required = false) Integer subjectMembershipId,
            @RequestParam(required = false) Integer teachingLoadTypeId,
            Authentication authentication) {
        requireStaff(authentication);
        if (!accessService.isAdmin(authentication)) {
            if (subjectMembershipId == null) {
                throw new AccessDeniedException("Teacher load-type queries require subjectMembershipId.");
            }
            accessService.requireSubjectMembershipOwner(authentication, subjectMembershipId);
        }
        return ApiResponses.list(
                teachingService.findSubjectLoadTypes(subjectMembershipId, teachingLoadTypeId),
                ApiResponses::subjectMembershipLoadType
        );
    }

    @PostMapping("/subject-memberships/{subjectMembershipId}/load-types")
    public ApiResponses.SubjectMembershipLoadTypeResponse addSubjectLoadType(
            @PathVariable Integer subjectMembershipId,
            @Valid @RequestBody SubjectLoadTypeRequest request,
            Authentication authentication) {
        accessService.requireSubjectMembershipOwner(authentication, subjectMembershipId);
        return ApiResponses.subjectMembershipLoadType(
                teachingService.addSubjectLoadType(subjectMembershipId, request.teachingLoadTypeId(), request.notes())
        );
    }

    @PutMapping("/subject-load-types/{subjectLoadTypeId}/status")
    public ApiResponses.SubjectMembershipLoadTypeResponse updateSubjectLoadTypeStatus(
            @PathVariable Integer subjectLoadTypeId,
            @Valid @RequestBody StatusRequest request,
            Authentication authentication) {
        accessService.requireSubjectLoadTypeOwner(authentication, subjectLoadTypeId);
        return ApiResponses.subjectMembershipLoadType(teachingService.updateSubjectLoadTypeStatus(subjectLoadTypeId, request.status()));
    }

    @PutMapping("/subject-load-types/{subjectLoadTypeId}")
    public ApiResponses.SubjectMembershipLoadTypeResponse updateSubjectLoadType(
            @PathVariable Integer subjectLoadTypeId,
            @Valid @RequestBody SubjectLoadTypeUpdateRequest request,
            Authentication authentication) {
        accessService.requireSubjectLoadTypeOwner(authentication, subjectLoadTypeId);
        return ApiResponses.subjectMembershipLoadType(
                teachingService.updateSubjectLoadType(subjectLoadTypeId, request.status(), request.notes())
        );
    }

    @GetMapping("/assignments")
    public List<ApiResponses.TeachingAssignmentResponse> assignments(@RequestParam(required = false) Integer groupId,
                                                                     @RequestParam(required = false) Integer subjectMembershipId,
                                                                     @RequestParam(required = false) Integer courseVersionId,
                                                                     @RequestParam(required = false) Integer facultyId,
                                                                     @RequestParam(required = false) Integer loadTypeId,
                                                                     @RequestParam(required = false) Integer studyCourse,
                                                                     @RequestParam(required = false) Integer semester,
                                                                     @RequestParam(required = false) Integer academicYear,
                                                                     @RequestParam(required = false) Integer status,
                                                                     Authentication authentication) {
        if (!accessService.isAdmin(authentication)) {
            if (subjectMembershipId != null && accessService.isTeacher(authentication)) {
                accessService.requireSubjectMembershipOwner(authentication, subjectMembershipId);
            } else if (groupId != null) {
                accessService.requireGroupMember(authentication, groupId);
            } else {
                throw new AccessDeniedException(
                        "Assignment queries require an owned subjectMembershipId or current groupId."
                );
            }
        }
        return ApiResponses.list(
                teachingService.findAssignments(
                        groupId,
                        subjectMembershipId,
                        courseVersionId,
                        facultyId,
                        loadTypeId,
                        studyCourse,
                        semester,
                        academicYear,
                        status
                ),
                ApiResponses::teachingAssignment
        );
    }

    @GetMapping("/assignments/{id}")
    public ApiResponses.TeachingAssignmentResponse assignment(@PathVariable Integer id,
                                                              Authentication authentication) {
        accessService.requireTeachingAssignmentAccess(authentication, id);
        return ApiResponses.teachingAssignment(teachingService.getAssignment(id));
    }

    @PostMapping("/assignments")
    public ApiResponses.TeachingAssignmentResponse createAssignment(@Valid @RequestBody TeachingAssignmentRequest request,
                                                                    Authentication authentication) {
        accessService.requireSubjectMembershipOwner(authentication, request.subjectMembershipId());
        return ApiResponses.teachingAssignment(teachingService.createAssignment(
                request.subjectMembershipId(),
                request.groupId(),
                request.loadTypeId(),
                request.courseVersionId(),
                request.semester(),
                request.studyCourse(),
                request.academicYear(),
                request.hoursPerWeek(),
                request.status(),
                request.notes()
        ));
    }

    @PutMapping("/assignments/{assignmentId}")
    public ApiResponses.TeachingAssignmentResponse updateAssignment(@PathVariable Integer assignmentId,
                                                                    @Valid @RequestBody TeachingAssignmentRequest request,
                                                                    Authentication authentication) {
        accessService.requireTeachingAssignmentOwner(authentication, assignmentId);
        accessService.requireSubjectMembershipOwner(authentication, request.subjectMembershipId());
        return ApiResponses.teachingAssignment(teachingService.updateAssignment(
                assignmentId,
                request.subjectMembershipId(),
                request.groupId(),
                request.loadTypeId(),
                request.courseVersionId(),
                request.semester(),
                request.studyCourse(),
                request.academicYear(),
                request.hoursPerWeek(),
                request.status(),
                request.notes()
        ));
    }

    @PutMapping("/assignments/{assignmentId}/status")
    public ApiResponses.TeachingAssignmentResponse updateAssignmentStatus(@PathVariable Integer assignmentId,
                                                                          @Valid @RequestBody StatusRequest request,
                                                                          Authentication authentication) {
        accessService.requireTeachingAssignmentOwner(authentication, assignmentId);
        return ApiResponses.teachingAssignment(teachingService.updateAssignmentStatus(assignmentId, request.status()));
    }

    @GetMapping("/enrollments")
    public List<ApiResponses.TeachingAssignmentEnrollmentResponse> enrollments(
            @RequestParam(required = false) Integer teachingAssignmentId,
            @RequestParam(required = false) Integer groupMembershipId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer status,
            Authentication authentication) {
        if (!accessService.isAdmin(authentication)) {
            if (teachingAssignmentId != null && accessService.isTeacher(authentication)) {
                accessService.requireTeachingAssignmentOwner(authentication, teachingAssignmentId);
            } else if (groupMembershipId != null) {
                accessService.requireGroupMembershipOwner(authentication, groupMembershipId);
            } else {
                throw new AccessDeniedException(
                        "Enrollment queries require an owned teachingAssignmentId or groupMembershipId."
                );
            }
        }
        return ApiResponses.list(
                teachingService.findEnrollments(teachingAssignmentId, groupMembershipId, groupId, status),
                ApiResponses::teachingAssignmentEnrollment
        );
    }

    @PostMapping("/assignments/{assignmentId}/enrollments")
    public ApiResponses.TeachingAssignmentEnrollmentResponse enroll(@PathVariable Integer assignmentId,
                                                                    @Valid @RequestBody EnrollmentRequest request,
                                                                    Authentication authentication) {
        accessService.requireTeachingAssignmentOwner(authentication, assignmentId);
        return ApiResponses.teachingAssignmentEnrollment(teachingService.enroll(assignmentId, request.groupMembershipId(), request.status()));
    }

    @PutMapping("/enrollments/{enrollmentId}/status")
    public ApiResponses.TeachingAssignmentEnrollmentResponse updateEnrollmentStatus(
            @PathVariable Integer enrollmentId,
            @Valid @RequestBody StatusRequest request,
            Authentication authentication) {
        accessService.requireEnrollmentAccess(authentication, enrollmentId);
        return ApiResponses.teachingAssignmentEnrollment(teachingService.updateEnrollmentStatus(enrollmentId, request.status()));
    }

    @GetMapping("/lecture-assignments")
    public List<ApiResponses.LectureAssignmentResponse> lectureAssignments(
            @RequestParam(required = false) Integer teachingAssignmentId,
            @RequestParam(required = false) Integer courseLectureId,
            Authentication authentication) {
        requireStaff(authentication);
        if (!accessService.isAdmin(authentication)) {
            if (teachingAssignmentId != null) {
                accessService.requireTeachingAssignmentOwner(authentication, teachingAssignmentId);
            } else if (courseLectureId != null) {
                accessService.requireLectureOwner(authentication, courseLectureId);
            } else {
                throw new AccessDeniedException("Lecture assignment queries require an owned filter.");
            }
        }
        return ApiResponses.list(
                teachingService.findLectureAssignments(teachingAssignmentId, courseLectureId),
                ApiResponses::lectureAssignment
        );
    }

    @PostMapping("/assignments/{assignmentId}/lecture-assignments")
    public ApiResponses.LectureAssignmentResponse assignLecture(@PathVariable Integer assignmentId,
                                                                @Valid @RequestBody LectureAssignmentRequest request,
                                                                Authentication authentication) {
        accessService.requireTeachingAssignmentOwner(authentication, assignmentId);
        accessService.requireLectureOwner(authentication, request.courseLectureId());
        return ApiResponses.lectureAssignment(teachingService.assignLecture(
                assignmentId,
                request.courseLectureId(),
                request.availableFromUtc(),
                request.dueToUtc(),
                request.closedAtUtc(),
                request.required(),
                request.minProgressPercent(),
                request.status()
        ));
    }

    @PutMapping("/lecture-assignments/{lectureAssignmentId}")
    public ApiResponses.LectureAssignmentResponse updateLectureAssignment(
            @PathVariable Integer lectureAssignmentId,
            @Valid @RequestBody LectureAssignmentRequest request,
            Authentication authentication) {
        accessService.requireLectureAssignmentOwner(authentication, lectureAssignmentId);
        accessService.requireLectureOwner(authentication, request.courseLectureId());
        return ApiResponses.lectureAssignment(teachingService.updateLectureAssignment(
                lectureAssignmentId,
                request.courseLectureId(),
                request.availableFromUtc(),
                request.dueToUtc(),
                request.closedAtUtc(),
                request.required(),
                request.minProgressPercent(),
                request.status()
        ));
    }

    @PutMapping("/lecture-assignments/{lectureAssignmentId}/status")
    public ApiResponses.LectureAssignmentResponse updateLectureAssignmentStatus(
            @PathVariable Integer lectureAssignmentId,
            @Valid @RequestBody StatusRequest request,
            Authentication authentication) {
        accessService.requireLectureAssignmentOwner(authentication, lectureAssignmentId);
        return ApiResponses.lectureAssignment(teachingService.updateLectureAssignmentStatus(lectureAssignmentId, request.status()));
    }

    @GetMapping("/lecture-progress")
    public List<ApiResponses.StudentLectureProgressResponse> progress(
            @RequestParam(required = false) Integer lectureAssignmentId,
            @RequestParam(required = false) Integer teachingAssignmentEnrollmentId,
            Authentication authentication) {
        requireStaff(authentication);
        if (!accessService.isAdmin(authentication)) {
            if (lectureAssignmentId != null) {
                accessService.requireLectureAssignmentOwner(authentication, lectureAssignmentId);
            } else if (teachingAssignmentEnrollmentId != null) {
                accessService.requireEnrollmentAccess(authentication, teachingAssignmentEnrollmentId);
            } else {
                throw new AccessDeniedException("Progress queries require an owned filter.");
            }
        }
        return ApiResponses.list(
                teachingService.findProgress(lectureAssignmentId, teachingAssignmentEnrollmentId),
                ApiResponses::studentLectureProgress
        );
    }

    @PostMapping("/lecture-assignments/{lectureAssignmentId}/progress")
    public ApiResponses.StudentLectureProgressResponse updateProgress(@PathVariable Integer lectureAssignmentId,
                                                                      @Valid @RequestBody ProgressRequest request,
                                                                      Authentication authentication) {
        accessService.requireLectureAssignmentOwner(authentication, lectureAssignmentId);
        accessService.requireEnrollmentAccess(authentication, request.teachingAssignmentEnrollmentId());
        return ApiResponses.studentLectureProgress(teachingService.updateProgress(
                lectureAssignmentId,
                request.teachingAssignmentEnrollmentId(),
                request.progressPercent(),
                request.status(),
                request.completionSource(),
                accessService.currentPersonId(authentication),
                request.lastPositionSeconds(),
                request.timeSpentSeconds()
        ));
    }

    public record LoadTypeRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 1000) String description
    ) {
    }

    public record SubjectLoadTypeRequest(
            @NotNull Integer teachingLoadTypeId,
            @Size(max = 1000) String notes
    ) {
    }

    public record SubjectLoadTypeUpdateRequest(
            @Min(1) @Max(3) int status,
            @Size(max = 1000) String notes
    ) {
    }

    public record TeachingAssignmentRequest(
            @NotNull Integer subjectMembershipId,
            @NotNull Integer groupId,
            @NotNull Integer loadTypeId,
            Integer courseVersionId,
            @Min(1) @Max(2) int semester,
            @Min(1) Integer studyCourse,
            @Min(2000) int academicYear,
            @DecimalMin("0.00") BigDecimal hoursPerWeek,
            @Min(1) @Max(4) Integer status,
            @Size(max = 1000) String notes
    ) {
    }

    public record EnrollmentRequest(
            @NotNull Integer groupMembershipId,
            @Min(1) @Max(4) Integer status
    ) {
    }

    public record LectureAssignmentRequest(
            @NotNull Integer courseLectureId,
            Instant availableFromUtc,
            Instant dueToUtc,
            Instant closedAtUtc,
            boolean required,
            @Min(0) @Max(100) int minProgressPercent,
            @Min(1) @Max(4) Integer status
    ) {
    }

    public record ProgressRequest(
            @NotNull Integer teachingAssignmentEnrollmentId,
            @Min(0) @Max(100) int progressPercent,
            @Min(1) @Max(3) Integer status,
            @Min(1) @Max(3) Integer completionSource,
            @PositiveOrZero Long lastPositionSeconds,
            @PositiveOrZero Long timeSpentSeconds
    ) {
    }

    public record StatusRequest(
            @Min(1) @Max(4) int status
    ) {
    }

    private void requireStaff(Authentication authentication) {
        if (!accessService.isTeacherOrAdmin(authentication)) {
            throw new AccessDeniedException("This teaching resource is available to staff only.");
        }
    }
}
