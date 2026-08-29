package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureAssignment;
import org.santayn.testing.models.lecture.StudentLectureProgress;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.subject.SubjectMembershipLoadType;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.santayn.testing.models.teacher.TeachingAssignmentEnrollment;
import org.santayn.testing.models.teacher.TeachingLoadType;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.FacultySubjectRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.LectureAssignmentRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.StudentLectureProgressRepository;
import org.santayn.testing.repository.SubjectMembershipLoadTypeRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.TeachingAssignmentEnrollmentRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TeachingLoadTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeachingService {

    private final TeachingLoadTypeRepository teachingLoadTypeRepository;
    private final SubjectMembershipLoadTypeRepository subjectMembershipLoadTypeRepository;
    private final SubjectMembershipRepository subjectMembershipRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TeachingAssignmentEnrollmentRepository teachingAssignmentEnrollmentRepository;
    private final FacultySubjectRepository facultySubjectRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final CourseVersionRepository courseVersionRepository;
    private final LectureRepository lectureRepository;
    private final LectureAssignmentRepository lectureAssignmentRepository;
    private final StudentLectureProgressRepository studentLectureProgressRepository;
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public List<TeachingLoadType> findLoadTypes() {
        return teachingLoadTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TeachingLoadType getLoadType(Integer id) {
        return teachingLoadTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teaching load type not found: " + id));
    }

    @Transactional
    public TeachingLoadType createLoadType(String name, String description) {
        String normalizedName = FacultyService.requireText(name, "Name");
        teachingLoadTypeRepository.findByName(normalizedName).ifPresent(existing -> {
            throw new AuthConflictException("Teaching load type already exists: " + normalizedName);
        });

        TeachingLoadType loadType = new TeachingLoadType();
        loadType.setName(normalizedName);
        loadType.setDescription(FacultyService.trimToNull(description));
        return teachingLoadTypeRepository.save(loadType);
    }

    @Transactional
    public TeachingLoadType updateLoadType(Integer id, String name, String description) {
        TeachingLoadType loadType = getLoadType(id);
        String normalizedName = FacultyService.requireText(name, "Name");
        if (!normalizedName.equals(loadType.getName()) && teachingLoadTypeRepository.existsByNameAndIdNot(normalizedName, id)) {
            throw new AuthConflictException("Teaching load type already exists: " + normalizedName);
        }

        loadType.setName(normalizedName);
        loadType.setDescription(FacultyService.trimToNull(description));
        return loadType;
    }

    @Transactional(readOnly = true)
    public List<SubjectMembershipLoadType> findSubjectLoadTypes(Integer subjectMembershipId, Integer teachingLoadTypeId) {
        if (subjectMembershipId != null && teachingLoadTypeId != null) {
            return subjectMembershipLoadTypeRepository
                    .findBySubjectMembershipIdAndTeachingLoadTypeIdAndRemovedAtUtcIsNull(subjectMembershipId, teachingLoadTypeId)
                    .map(List::of)
                    .orElseGet(List::of);
        }
        if (subjectMembershipId != null) {
            return subjectMembershipLoadTypeRepository.findBySubjectMembershipIdAndRemovedAtUtcIsNull(subjectMembershipId);
        }
        if (teachingLoadTypeId != null) {
            return subjectMembershipLoadTypeRepository.findByTeachingLoadTypeIdAndRemovedAtUtcIsNull(teachingLoadTypeId);
        }
        return subjectMembershipLoadTypeRepository.findAll();
    }

    @Transactional
    public SubjectMembershipLoadType addSubjectLoadType(Integer subjectMembershipId,
                                                        Integer teachingLoadTypeId,
                                                        String notes) {
        requireSubjectMembership(subjectMembershipId);
        requireLoadType(teachingLoadTypeId);

        return subjectMembershipLoadTypeRepository
                .findBySubjectMembershipIdAndTeachingLoadTypeIdAndRemovedAtUtcIsNull(subjectMembershipId, teachingLoadTypeId)
                .orElseGet(() -> {
                    SubjectMembershipLoadType subjectLoadType = new SubjectMembershipLoadType();
                    subjectLoadType.setSubjectMembershipId(subjectMembershipId);
                    subjectLoadType.setTeachingLoadTypeId(teachingLoadTypeId);
                    subjectLoadType.setStatus(1);
                    subjectLoadType.setAssignedAtUtc(Instant.now());
                    subjectLoadType.setNotes(FacultyService.trimToNull(notes));
                    return subjectMembershipLoadTypeRepository.save(subjectLoadType);
                });
    }

    @Transactional
    public SubjectMembershipLoadType updateSubjectLoadTypeStatus(Integer subjectLoadTypeId, int status) {
        SubjectMembershipLoadType subjectLoadType = subjectMembershipLoadTypeRepository.findById(subjectLoadTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Subject membership load type not found: " + subjectLoadTypeId));
        int normalizedStatus = normalizeStatus(status, 1, 3, 1);
        subjectLoadType.setStatus(normalizedStatus);
        subjectLoadType.setRemovedAtUtc(normalizedStatus == 3 ? Instant.now() : null);
        return subjectLoadType;
    }

    @Transactional
    public SubjectMembershipLoadType updateSubjectLoadType(Integer subjectLoadTypeId, int status, String notes) {
        SubjectMembershipLoadType subjectLoadType = subjectMembershipLoadTypeRepository.findById(subjectLoadTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Subject membership load type not found: " + subjectLoadTypeId));
        int normalizedStatus = normalizeStatus(status, 1, 3, 1);
        subjectLoadType.setStatus(normalizedStatus);
        subjectLoadType.setRemovedAtUtc(normalizedStatus == 3 ? Instant.now() : null);
        subjectLoadType.setNotes(FacultyService.trimToNull(notes));
        return subjectLoadType;
    }

    @Transactional(readOnly = true)
    public List<TeachingAssignment> findAssignments(Integer groupId,
                                                    Integer subjectMembershipId,
                                                    Integer courseVersionId,
                                                    Integer facultyId,
                                                    Integer loadTypeId,
                                                    Integer studyCourse,
                                                    Integer semester,
                                                    Integer academicYear,
                                                    Integer status) {
        return teachingAssignmentRepository.findByFilters(
                groupId,
                subjectMembershipId,
                courseVersionId,
                facultyId,
                loadTypeId,
                studyCourse,
                semester,
                academicYear,
                status
        );
    }

    @Transactional(readOnly = true)
    public TeachingAssignment getAssignment(Integer id) {
        return teachingAssignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teaching assignment not found: " + id));
    }

    @Transactional
    public TeachingAssignment createAssignment(Integer subjectMembershipId,
                                               Integer groupId,
                                               Integer loadTypeId,
                                               Integer courseVersionId,
                                               int semester,
                                               Integer studyCourse,
                                               int academicYear,
                                               BigDecimal hoursPerWeek,
                                               Integer status,
                                               String notes) {
        SubjectMembership subjectMembership = requireSubjectMembership(subjectMembershipId);
        Group group = requireGroup(groupId);
        requireLoadType(loadTypeId);
        requireSubjectAssignedToFaculty(group.getFacultyId(), subjectMembership.getSubjectId());
        requireCourseVersionMatchesSubject(courseVersionId, subjectMembership.getSubjectId());
        requireSubjectLoadType(subjectMembershipId, loadTypeId);
        requireSemester(semester);
        requireStudyCourse(studyCourse);
        requireAcademicYear(academicYear);
        if (teachingAssignmentRepository.existsBySubjectIdAndGroupIdAndStudyCourseAndAcademicYearAndSemester(
                subjectMembership.getSubjectId(),
                groupId,
                studyCourse,
                academicYear,
                semester
        )) {
            throw new AuthConflictException("Teaching assignment already exists for this subject, group and term.");
        }

        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setSubjectMembershipId(subjectMembershipId);
        assignment.setGroupId(groupId);
        assignment.setLoadTypeId(loadTypeId);
        assignment.setCourseVersionId(courseVersionId);
        assignment.setSemester(semester);
        assignment.setStudyCourse(studyCourse);
        assignment.setAcademicYear(academicYear);
        assignment.setHoursPerWeek(requireNonNegative(hoursPerWeek, "HoursPerWeek"));
        assignment.setStatus(normalizeStatus(status, 1, 4, 1));
        assignment.setNotes(FacultyService.trimToNull(notes));
        return teachingAssignmentRepository.save(assignment);
    }

    @Transactional
    public TeachingAssignment updateAssignment(Integer teachingAssignmentId,
                                               Integer subjectMembershipId,
                                               Integer groupId,
                                               Integer loadTypeId,
                                               Integer courseVersionId,
                                               int semester,
                                               Integer studyCourse,
                                               int academicYear,
                                               BigDecimal hoursPerWeek,
                                               Integer status,
                                               String notes) {
        TeachingAssignment assignment = getAssignment(teachingAssignmentId);
        SubjectMembership subjectMembership = requireSubjectMembership(subjectMembershipId);
        Group group = requireGroup(groupId);
        requireLoadType(loadTypeId);
        requireSubjectAssignedToFaculty(group.getFacultyId(), subjectMembership.getSubjectId());
        requireCourseVersionMatchesSubject(courseVersionId, subjectMembership.getSubjectId());
        requireSubjectLoadType(subjectMembershipId, loadTypeId);
        requireSemester(semester);
        requireStudyCourse(studyCourse);
        requireAcademicYear(academicYear);
        if (teachingAssignmentRepository.existsBySubjectIdAndGroupIdAndStudyCourseAndAcademicYearAndSemesterAndIdNot(
                subjectMembership.getSubjectId(),
                groupId,
                studyCourse,
                academicYear,
                semester,
                teachingAssignmentId
        )) {
            throw new AuthConflictException("Teaching assignment already exists for this subject, group and term.");
        }

        assignment.setSubjectMembershipId(subjectMembershipId);
        assignment.setGroupId(groupId);
        assignment.setLoadTypeId(loadTypeId);
        assignment.setCourseVersionId(courseVersionId);
        assignment.setSemester(semester);
        assignment.setStudyCourse(studyCourse);
        assignment.setAcademicYear(academicYear);
        assignment.setHoursPerWeek(requireNonNegative(hoursPerWeek, "HoursPerWeek"));
        assignment.setStatus(normalizeStatus(status, 1, 4, assignment.getStatus()));
        assignment.setNotes(FacultyService.trimToNull(notes));
        return assignment;
    }

    @Transactional
    public TeachingAssignment updateAssignmentStatus(Integer teachingAssignmentId, int status) {
        TeachingAssignment assignment = getAssignment(teachingAssignmentId);
        assignment.setStatus(normalizeStatus(status, 1, 4, 1));
        return assignment;
    }

    @Transactional(readOnly = true)
    public List<TeachingAssignmentEnrollment> findEnrollments(Integer teachingAssignmentId,
                                                              Integer groupMembershipId,
                                                              Integer groupId,
                                                              Integer status) {
        return teachingAssignmentEnrollmentRepository.findByFilters(teachingAssignmentId, groupMembershipId, groupId, status);
    }

    @Transactional
    public TeachingAssignmentEnrollment enroll(Integer teachingAssignmentId, Integer groupMembershipId, Integer status) {
        TeachingAssignment assignment = getAssignment(teachingAssignmentId);
        GroupMembership groupMembership = groupMembershipRepository.findById(groupMembershipId)
                .orElseThrow(() -> new IllegalArgumentException("Group membership not found: " + groupMembershipId));
        if (!assignment.getGroupId().equals(groupMembership.getGroupId())) {
            throw new IllegalArgumentException("Group membership does not belong to assignment group.");
        }

        return teachingAssignmentEnrollmentRepository
                .findByFilters(teachingAssignmentId, groupMembershipId, assignment.getGroupId(), null)
                .stream()
                .filter(existing -> existing.getRemovedAtUtc() == null && (existing.getStatus() == 1 || existing.getStatus() == 2))
                .findFirst()
                .orElseGet(() -> {
                    TeachingAssignmentEnrollment enrollment = new TeachingAssignmentEnrollment();
                    int normalizedStatus = normalizeStatus(status, 1, 4, 1);
                    enrollment.setTeachingAssignmentId(teachingAssignmentId);
                    enrollment.setGroupMembershipId(groupMembershipId);
                    enrollment.setGroupId(groupMembership.getGroupId());
                    enrollment.setStatus(normalizedStatus);
                    enrollment.setEnrolledAtUtc(Instant.now());
                    if (normalizedStatus == 3 || normalizedStatus == 4) {
                        enrollment.setRemovedAtUtc(Instant.now());
                    }
                    return teachingAssignmentEnrollmentRepository.save(enrollment);
                });
    }

    @Transactional
    public TeachingAssignmentEnrollment updateEnrollmentStatus(Integer enrollmentId, int status) {
        TeachingAssignmentEnrollment enrollment = teachingAssignmentEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Teaching assignment enrollment not found: " + enrollmentId));
        int normalizedStatus = normalizeStatus(status, 1, 4, 1);
        enrollment.setStatus(normalizedStatus);
        enrollment.setRemovedAtUtc(normalizedStatus == 3 || normalizedStatus == 4 ? Instant.now() : null);
        return enrollment;
    }

    @Transactional(readOnly = true)
    public List<LectureAssignment> findLectureAssignments(Integer teachingAssignmentId, Integer courseLectureId) {
        if (teachingAssignmentId != null && courseLectureId != null) {
            return lectureAssignmentRepository
                    .findByTeachingAssignmentIdAndCourseLectureId(teachingAssignmentId, courseLectureId)
                    .map(List::of)
                    .orElseGet(List::of);
        }
        if (teachingAssignmentId != null) {
            return lectureAssignmentRepository.findByTeachingAssignmentId(teachingAssignmentId);
        }
        if (courseLectureId != null) {
            return lectureAssignmentRepository.findByCourseLectureId(courseLectureId);
        }
        return lectureAssignmentRepository.findAll();
    }

    @Transactional
    public LectureAssignment assignLecture(Integer teachingAssignmentId,
                                           Integer courseLectureId,
                                           Instant availableFromUtc,
                                           Instant dueToUtc,
                                           Instant closedAtUtc,
                                           boolean required,
                                           int minProgressPercent,
                                           Integer status) {
        TeachingAssignment assignment = getAssignment(teachingAssignmentId);
        SubjectMembership subjectMembership = requireSubjectMembership(assignment.getSubjectMembershipId());
        Lecture lecture = lectureRepository.findById(courseLectureId)
                .orElseThrow(() -> new IllegalArgumentException("Course lecture not found: " + courseLectureId));
        requireLectureMatchesAssignment(assignment, subjectMembership, lecture);
        requireTimeRange(availableFromUtc, dueToUtc, closedAtUtc);
        requirePercent(minProgressPercent, "MinProgressPercent");

        return lectureAssignmentRepository
                .findByTeachingAssignmentIdAndCourseLectureId(teachingAssignmentId, courseLectureId)
                .orElseGet(() -> {
                    LectureAssignment lectureAssignment = new LectureAssignment();
                    lectureAssignment.setTeachingAssignmentId(teachingAssignmentId);
                    lectureAssignment.setCourseLectureId(courseLectureId);
                    lectureAssignment.setAvailableFromUtc(availableFromUtc);
                    lectureAssignment.setDueToUtc(dueToUtc);
                    lectureAssignment.setClosedAtUtc(closedAtUtc);
                    lectureAssignment.setRequired(required);
                    lectureAssignment.setMinProgressPercent(minProgressPercent);
                    lectureAssignment.setStatus(normalizeStatus(status, 1, 4, 1));
                    lectureAssignment.setCreatedAtUtc(Instant.now());
                    lectureAssignment.setSnapshotCourseVersionId(
                            lecture.getCourseVersionId() != null ? lecture.getCourseVersionId() : assignment.getCourseVersionId()
                    );
                    lectureAssignment.setSnapshotGroupId(assignment.getGroupId());
                    lectureAssignment.setSnapshotTeacherPersonId(subjectMembership.getPersonId());
                    lectureAssignment.setSnapshotSemester(assignment.getSemester());
                    lectureAssignment.setSnapshotAcademicYear(assignment.getAcademicYear());
                    return lectureAssignmentRepository.save(lectureAssignment);
                });
    }

    @Transactional
    public LectureAssignment updateLectureAssignmentStatus(Integer lectureAssignmentId, int status) {
        LectureAssignment lectureAssignment = lectureAssignmentRepository.findById(lectureAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture assignment not found: " + lectureAssignmentId));
        int normalizedStatus = normalizeStatus(status, 1, 4, 1);
        lectureAssignment.setStatus(normalizedStatus);
        if (normalizedStatus == 3 && lectureAssignment.getClosedAtUtc() == null) {
            lectureAssignment.setClosedAtUtc(Instant.now());
        }
        return lectureAssignment;
    }

    @Transactional
    public LectureAssignment updateLectureAssignment(Integer lectureAssignmentId,
                                                     Integer courseLectureId,
                                                     Instant availableFromUtc,
                                                     Instant dueToUtc,
                                                     Instant closedAtUtc,
                                                     boolean required,
                                                     int minProgressPercent,
                                                     Integer status) {
        LectureAssignment lectureAssignment = lectureAssignmentRepository.findById(lectureAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture assignment not found: " + lectureAssignmentId));
        TeachingAssignment assignment = getAssignment(lectureAssignment.getTeachingAssignmentId());
        SubjectMembership subjectMembership = requireSubjectMembership(assignment.getSubjectMembershipId());
        Lecture lecture = lectureRepository.findById(courseLectureId)
                .orElseThrow(() -> new IllegalArgumentException("Course lecture not found: " + courseLectureId));
        requireLectureMatchesAssignment(assignment, subjectMembership, lecture);
        lectureAssignmentRepository
                .findByTeachingAssignmentIdAndCourseLectureId(lectureAssignment.getTeachingAssignmentId(), courseLectureId)
                .filter(existing -> !existing.getId().equals(lectureAssignmentId))
                .ifPresent(existing -> {
                    throw new AuthConflictException("Lecture assignment already exists for this lecture and teaching assignment.");
                });
        requireTimeRange(availableFromUtc, dueToUtc, closedAtUtc);
        requirePercent(minProgressPercent, "MinProgressPercent");

        lectureAssignment.setCourseLectureId(courseLectureId);
        lectureAssignment.setAvailableFromUtc(availableFromUtc);
        lectureAssignment.setDueToUtc(dueToUtc);
        lectureAssignment.setClosedAtUtc(closedAtUtc);
        lectureAssignment.setRequired(required);
        lectureAssignment.setMinProgressPercent(minProgressPercent);
        lectureAssignment.setStatus(normalizeStatus(status, 1, 4, lectureAssignment.getStatus()));
        lectureAssignment.setSnapshotCourseVersionId(
                lecture.getCourseVersionId() != null ? lecture.getCourseVersionId() : assignment.getCourseVersionId()
        );
        lectureAssignment.setSnapshotGroupId(assignment.getGroupId());
        lectureAssignment.setSnapshotTeacherPersonId(subjectMembership.getPersonId());
        lectureAssignment.setSnapshotSemester(assignment.getSemester());
        lectureAssignment.setSnapshotAcademicYear(assignment.getAcademicYear());
        return lectureAssignment;
    }

    @Transactional(readOnly = true)
    public List<StudentLectureProgress> findProgress(Integer lectureAssignmentId, Integer teachingAssignmentEnrollmentId) {
        if (lectureAssignmentId != null && teachingAssignmentEnrollmentId != null) {
            return studentLectureProgressRepository
                    .findByLectureAssignmentIdAndTeachingAssignmentEnrollmentId(lectureAssignmentId, teachingAssignmentEnrollmentId)
                    .map(List::of)
                    .orElseGet(List::of);
        }
        if (lectureAssignmentId != null) {
            return studentLectureProgressRepository.findByLectureAssignmentId(lectureAssignmentId);
        }
        if (teachingAssignmentEnrollmentId != null) {
            return studentLectureProgressRepository.findByTeachingAssignmentEnrollmentId(teachingAssignmentEnrollmentId);
        }
        return studentLectureProgressRepository.findAll();
    }

    @Transactional
    public StudentLectureProgress updateProgress(Integer lectureAssignmentId,
                                                 Integer teachingAssignmentEnrollmentId,
                                                 int progressPercent,
                                                 Integer status,
                                                 Integer completionSource,
                                                 Integer completedByPersonId,
                                                 Long lastPositionSeconds,
                                                 Long timeSpentSeconds) {
        LectureAssignment lectureAssignment = lectureAssignmentRepository.findById(lectureAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture assignment not found: " + lectureAssignmentId));
        TeachingAssignmentEnrollment enrollment = teachingAssignmentEnrollmentRepository.findById(teachingAssignmentEnrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Teaching assignment enrollment not found: " + teachingAssignmentEnrollmentId));
        if (!lectureAssignment.getTeachingAssignmentId().equals(enrollment.getTeachingAssignmentId())) {
            throw new IllegalArgumentException("Enrollment does not belong to lecture assignment teaching assignment.");
        }
        requirePercent(progressPercent, "ProgressPercent");
        if (completedByPersonId != null && !personRepository.existsById(completedByPersonId)) {
            throw new IllegalArgumentException("Person not found: " + completedByPersonId);
        }
        requireNonNegative(lastPositionSeconds, "LastPositionSeconds");
        requireNonNegative(timeSpentSeconds, "TimeSpentSeconds");

        Instant now = Instant.now();
        StudentLectureProgress progress = studentLectureProgressRepository
                .findByLectureAssignmentIdAndTeachingAssignmentEnrollmentId(lectureAssignmentId, teachingAssignmentEnrollmentId)
                .orElseGet(() -> {
                    StudentLectureProgress created = new StudentLectureProgress();
                    created.setLectureAssignmentId(lectureAssignmentId);
                    created.setTeachingAssignmentEnrollmentId(teachingAssignmentEnrollmentId);
                    created.setCreatedAtUtc(now);
                    return created;
                });

        int normalizedStatus = normalizeProgressStatus(status, progressPercent, lectureAssignment.getMinProgressPercent());
        progress.setProgressPercent(progressPercent);
        progress.setStatus(normalizedStatus);
        if (progress.getFirstOpenedAtUtc() == null) {
            progress.setFirstOpenedAtUtc(now);
        }
        progress.setLastVisitedAtUtc(now);
        progress.setLastPositionSeconds(lastPositionSeconds);
        progress.setTimeSpentSeconds(timeSpentSeconds);
        if (normalizedStatus == 2) {
            if (progress.getCompletedAtUtc() == null) {
                progress.setCompletedAtUtc(now);
            }
            progress.setCompletionSource(completionSource == null ? 1 : completionSource);
            progress.setCompletedByPersonId(completedByPersonId);
        }
        progress.setUpdatedAtUtc(now);
        return studentLectureProgressRepository.save(progress);
    }

    private SubjectMembership requireSubjectMembership(Integer subjectMembershipId) {
        return subjectMembershipRepository.findById(subjectMembershipId)
                .orElseThrow(() -> new IllegalArgumentException("Subject membership not found: " + subjectMembershipId));
    }

    private Group requireGroup(Integer groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
    }

    private void requireLoadType(Integer loadTypeId) {
        if (loadTypeId == null || !teachingLoadTypeRepository.existsById(loadTypeId)) {
            throw new IllegalArgumentException("Teaching load type not found: " + loadTypeId);
        }
    }

    private void requireSubjectLoadType(Integer subjectMembershipId, Integer loadTypeId) {
        subjectMembershipLoadTypeRepository
                .findBySubjectMembershipIdAndTeachingLoadTypeIdAndRemovedAtUtcIsNull(subjectMembershipId, loadTypeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subject membership load type not found: " + subjectMembershipId + "/" + loadTypeId
                ));
    }

    private void requireSubjectAssignedToFaculty(Integer facultyId, Integer subjectId) {
        if (!facultySubjectRepository.existsByFacultyIdAndSubjectId(facultyId, subjectId)) {
            throw new IllegalArgumentException(
                    "Subject " + subjectId + " is not assigned to faculty " + facultyId + "."
            );
        }
    }

    private void requireCourseVersionMatchesSubject(Integer courseVersionId, Integer subjectId) {
        if (courseVersionId == null) {
            return;
        }
        Integer courseVersionSubjectId = courseVersionRepository.findSubjectIdByCourseVersionId(courseVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Course version not found: " + courseVersionId));
        if (!courseVersionSubjectId.equals(subjectId)) {
            throw new IllegalArgumentException(
                    "Course version " + courseVersionId + " does not belong to subject " + subjectId + "."
            );
        }
    }

    private void requireLectureMatchesAssignment(TeachingAssignment assignment,
                                                 SubjectMembership subjectMembership,
                                                 Lecture lecture) {
        Integer lectureSubjectId = lecture.getSubjectId();
        if (lectureSubjectId == null) {
            if (lecture.getCourseVersionId() == null) {
                throw new IllegalArgumentException("Course lecture is not bound to a subject.");
            }
            requireCourseVersionMatchesSubject(lecture.getCourseVersionId(), subjectMembership.getSubjectId());
            lectureSubjectId = subjectMembership.getSubjectId();
        }
        if (!subjectMembership.getSubjectId().equals(lectureSubjectId)) {
            throw new IllegalArgumentException("Course lecture does not belong to assignment subject.");
        }
        if (lecture.getSubjectMembershipId() != null
                && !lecture.getSubjectMembershipId().equals(assignment.getSubjectMembershipId())) {
            throw new IllegalArgumentException("Course lecture belongs to another teacher subject membership.");
        }
        if (assignment.getCourseVersionId() != null
                && lecture.getCourseVersionId() != null
                && !assignment.getCourseVersionId().equals(lecture.getCourseVersionId())) {
            throw new IllegalArgumentException("Course lecture does not belong to assignment course version.");
        }
    }

    private static void requireSemester(int semester) {
        if (semester != 1 && semester != 2) {
            throw new IllegalArgumentException("Semester must be 1 or 2.");
        }
    }

    private static void requireStudyCourse(Integer studyCourse) {
        if (studyCourse != null && studyCourse < 1) {
            throw new IllegalArgumentException("StudyCourse must be greater than or equal to 1.");
        }
    }

    private static void requireAcademicYear(int academicYear) {
        if (academicYear < 2000) {
            throw new IllegalArgumentException("AcademicYear must be greater than or equal to 2000.");
        }
    }

    private static void requirePercent(int value, String field) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(field + " must be between 0 and 100.");
        }
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        BigDecimal actual = value == null ? BigDecimal.ZERO : value;
        if (actual.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " must be non-negative.");
        }
        return actual;
    }

    private static void requireNonNegative(Long value, String field) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative.");
        }
    }

    private static int normalizeStatus(Integer status, int min, int max, int defaultStatus) {
        int actual = status == null || status == 0 ? defaultStatus : status;
        if (actual < min || actual > max) {
            throw new IllegalArgumentException("Status must be between " + min + " and " + max + ".");
        }
        return actual;
    }

    private static int normalizeProgressStatus(Integer status, int progressPercent, int minProgressPercent) {
        if (status != null && status != 0) {
            return normalizeStatus(status, 1, 3, 1);
        }
        return progressPercent >= minProgressPercent ? 2 : 1;
    }

    private static void requireTimeRange(Instant availableFromUtc, Instant dueToUtc, Instant closedAtUtc) {
        if (availableFromUtc != null && dueToUtc != null && !availableFromUtc.isBefore(dueToUtc)) {
            throw new IllegalArgumentException("AvailableFromUtc must be before DueToUtc.");
        }
        if (dueToUtc != null && closedAtUtc != null && dueToUtc.isAfter(closedAtUtc)) {
            throw new IllegalArgumentException("DueToUtc must be before or equal to ClosedAtUtc.");
        }
        if (availableFromUtc != null && closedAtUtc != null && availableFromUtc.isAfter(closedAtUtc)) {
            throw new IllegalArgumentException("AvailableFromUtc must be before or equal to ClosedAtUtc.");
        }
    }
}
