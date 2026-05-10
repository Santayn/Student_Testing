package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureService {

    private final LectureRepository lectureRepository;
    private final CourseVersionRepository courseVersionRepository;
    private final SubjectMembershipRepository subjectMembershipRepository;
    private final TestRepository testRepository;
    private final LectureTestLinkService lectureTestLinkService;

    @Transactional(readOnly = true)
    public List<Lecture> findAll(Integer subjectId, Integer subjectMembershipId, Integer courseVersionId) {
        if (subjectMembershipId != null) {
            return lectureRepository.findBySubjectMembershipIdOrderByOrdinalAsc(subjectMembershipId);
        }
        if (subjectId != null) {
            return lectureRepository.findBySubjectIdOrderByOrdinalAsc(subjectId);
        }
        if (courseVersionId != null) {
            return lectureRepository.findByCourseVersionIdOrderByOrdinalAsc(courseVersionId);
        }
        return lectureRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Lecture get(Integer id) {
        return lectureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found: " + id));
    }

    @Transactional
    public Lecture create(Integer subjectId,
                          Integer subjectMembershipId,
                          Integer courseVersionId,
                          int ordinal,
                          String title,
                          String description,
                          String contentFolderKey,
                          Integer linkedTestId,
                          boolean publicVisible) {
        LecturePlacement placement = resolvePlacement(subjectId, subjectMembershipId, courseVersionId);
        requireLinkedTest(linkedTestId);
        int normalizedOrdinal = Math.max(1, ordinal);
        requireLectureOrdinalAvailable(
                placement.subjectMembershipId(),
                placement.courseVersionId(),
                normalizedOrdinal,
                null
        );

        Lecture lecture = new Lecture();
        lecture.setSubjectId(placement.subjectId());
        lecture.setSubjectMembershipId(placement.subjectMembershipId());
        lecture.setCourseVersionId(placement.courseVersionId());
        lecture.setOrdinal(normalizedOrdinal);
        lecture.setTitle(FacultyService.requireText(title, "Title"));
        lecture.setDescription(FacultyService.trimToNull(description));
        lecture.setContentFolderKey(FacultyService.requireText(contentFolderKey, "ContentFolderKey"));
        lecture.setLinkedTestId(linkedTestId);
        lecture.setPublicVisible(publicVisible);
        Lecture savedLecture = lectureRepository.save(lecture);
        ensureLinkedTestAvailability(savedLecture);
        return savedLecture;
    }

    @Transactional
    public Lecture update(Integer lectureId,
                          Integer subjectId,
                          Integer subjectMembershipId,
                          Integer courseVersionId,
                          int ordinal,
                          String title,
                          String description,
                          String contentFolderKey,
                          Integer linkedTestId,
                          boolean publicVisible) {
        Lecture lecture = get(lectureId);
        LecturePlacement placement = resolvePlacement(subjectId, subjectMembershipId, courseVersionId);
        requireLinkedTest(linkedTestId);
        int normalizedOrdinal = Math.max(1, ordinal);
        requireLectureOrdinalAvailable(
                placement.subjectMembershipId(),
                placement.courseVersionId(),
                normalizedOrdinal,
                lectureId
        );

        lecture.setSubjectId(placement.subjectId());
        lecture.setSubjectMembershipId(placement.subjectMembershipId());
        lecture.setCourseVersionId(placement.courseVersionId());
        lecture.setOrdinal(normalizedOrdinal);
        lecture.setTitle(FacultyService.requireText(title, "Title"));
        lecture.setDescription(FacultyService.trimToNull(description));
        lecture.setContentFolderKey(FacultyService.requireText(contentFolderKey, "ContentFolderKey"));
        lecture.setLinkedTestId(linkedTestId);
        lecture.setPublicVisible(publicVisible);
        ensureLinkedTestAvailability(lecture);
        return lecture;
    }

    @Transactional
    public Lecture updateLinkedTest(Integer lectureId, Integer linkedTestId) {
        Lecture lecture = get(lectureId);
        requireLinkedTest(linkedTestId);
        lecture.setLinkedTestId(linkedTestId);
        ensureLinkedTestAvailability(lecture);
        return lecture;
    }

    @Transactional
    public void delete(Integer id) {
        Lecture lecture = get(id);
        lectureRepository.delete(lecture);
    }

    private LecturePlacement resolvePlacement(Integer subjectId,
                                             Integer subjectMembershipId,
                                             Integer courseVersionId) {
        if (subjectMembershipId != null) {
            SubjectMembership membership = subjectMembershipRepository.findById(subjectMembershipId)
                    .orElseThrow(() -> new IllegalArgumentException("Subject membership not found: " + subjectMembershipId));
            if (subjectId != null && !subjectId.equals(membership.getSubjectId())) {
                throw new IllegalArgumentException("Subject membership " + subjectMembershipId
                        + " does not belong to subject " + subjectId + ".");
            }
            if (courseVersionId != null) {
                requireCourseVersionMatchesSubject(courseVersionId, membership.getSubjectId());
            }
            return new LecturePlacement(membership.getSubjectId(), membership.getId(), courseVersionId);
        }

        if (courseVersionId == null) {
            throw new IllegalArgumentException("SubjectMembershipId or CourseVersionId is required.");
        }
        Integer resolvedSubjectId = courseVersionRepository.findSubjectIdByCourseVersionId(courseVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Course version not found: " + courseVersionId));
        if (subjectId != null && !subjectId.equals(resolvedSubjectId)) {
            throw new IllegalArgumentException("Course version " + courseVersionId
                    + " does not belong to subject " + subjectId + ".");
        }
        return new LecturePlacement(resolvedSubjectId, null, courseVersionId);
    }

    private void requireCourseVersionMatchesSubject(Integer courseVersionId, Integer subjectId) {
        Integer courseVersionSubjectId = courseVersionRepository.findSubjectIdByCourseVersionId(courseVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Course version not found: " + courseVersionId));
        if (!courseVersionSubjectId.equals(subjectId)) {
            throw new IllegalArgumentException(
                    "Course version " + courseVersionId + " does not belong to subject " + subjectId + "."
            );
        }
    }

    private void requireLectureOrdinalAvailable(Integer subjectMembershipId,
                                                Integer courseVersionId,
                                                int ordinal,
                                                Integer lectureId) {
        if (subjectMembershipId != null) {
            boolean exists = lectureId == null
                    ? lectureRepository.existsBySubjectMembershipIdAndOrdinal(subjectMembershipId, ordinal)
                    : lectureRepository.existsBySubjectMembershipIdAndOrdinalAndIdNot(subjectMembershipId, ordinal, lectureId);
            if (exists) {
                throw new AuthConflictException("Teacher lecture ordinal already exists: "
                        + subjectMembershipId + "/" + ordinal);
            }
            return;
        }

        if (courseVersionId != null) {
            boolean exists = lectureId == null
                    ? lectureRepository.existsByCourseVersionIdAndOrdinal(courseVersionId, ordinal)
                    : lectureRepository.existsByCourseVersionIdAndOrdinalAndIdNot(courseVersionId, ordinal, lectureId);
            if (exists) {
                throw new AuthConflictException("Course lecture ordinal already exists: "
                        + courseVersionId + "/" + ordinal);
            }
        }
    }

    private void requireLinkedTest(Integer linkedTestId) {
        if (linkedTestId != null && !testRepository.existsById(linkedTestId)) {
            throw new IllegalArgumentException("Test not found: " + linkedTestId);
        }
    }

    private void ensureLinkedTestAvailability(Lecture lecture) {
        if (lecture.getLinkedTestId() != null) {
            lectureTestLinkService.ensureLectureTestAvailability(lecture.getId(), lecture.getLinkedTestId());
        }
    }

    private record LecturePlacement(Integer subjectId, Integer subjectMembershipId, Integer courseVersionId) {
    }
}
