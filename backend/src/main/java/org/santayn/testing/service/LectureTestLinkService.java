package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureTestLink;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.LectureTestLinkRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LectureTestLinkService {

    private final LectureRepository lectureRepository;
    private final LectureTestLinkRepository lectureTestLinkRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final TestRepository testRepository;

    @Transactional(readOnly = true)
    public List<Test> findTestsByLectureId(Integer lectureId) {
        requireLecture(lectureId);
        return lectureTestLinkRepository.findByCourseLectureIdOrderByIdAsc(lectureId)
                .stream()
                .map(LectureTestLink::getTestId)
                .map(testRepository::findById)
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Integer> findTestIdsByLectureId(Integer lectureId) {
        requireLecture(lectureId);
        return lectureTestLinkRepository.findByCourseLectureIdOrderByIdAsc(lectureId)
                .stream()
                .map(LectureTestLink::getTestId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    @Transactional
    public List<Test> replaceLectureTests(Integer lectureId, List<Integer> testIds) {
        Lecture lecture = requireLecture(lectureId);
        Integer subjectId = lecture.getSubjectId() != null
                ? lecture.getSubjectId()
                : lectureRepository.findSubjectIdByLectureId(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture subject not found: " + lectureId));

        LinkedHashSet<Integer> selectedIds = (testIds == null ? List.<Integer>of() : testIds)
                .stream()
                .filter(id -> id != null && id > 0)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        for (Integer testId : selectedIds) {
            if (!testRepository.existsById(testId)) {
                throw new IllegalArgumentException("Test not found: " + testId);
            }
            if (!testRepository.existsInSubject(testId, subjectId)) {
                throw new IllegalArgumentException("Test " + testId + " does not belong to subject " + subjectId + ".");
            }
        }

        List<LectureTestLink> existingLinks = lectureTestLinkRepository.findByCourseLectureIdOrderByIdAsc(lectureId);
        LinkedHashMap<Integer, LectureTestLink> existingByTestId = new LinkedHashMap<>();
        existingLinks.forEach(link -> existingByTestId.put(link.getTestId(), link));

        existingLinks.stream()
                .filter(link -> !selectedIds.contains(link.getTestId()))
                .forEach(lectureTestLinkRepository::delete);

        selectedIds.stream()
                .filter(testId -> !existingByTestId.containsKey(testId))
                .forEach(testId -> {
                    LectureTestLink link = new LectureTestLink();
                    link.setCourseLectureId(lectureId);
                    link.setTestId(testId);
                    lectureTestLinkRepository.save(link);
                });

        selectedIds.forEach(testId -> ensureLectureTestAvailability(lectureId, testId));

        return findTestsByLectureId(lectureId);
    }

    @Transactional
    public TestAssignment ensureLectureTestAvailability(Integer lectureId, Integer testId) {
        requireLecture(lectureId);
        if (!testRepository.existsById(testId)) {
            throw new IllegalArgumentException("Test not found: " + testId);
        }

        Instant now = Instant.now();
        Instant defaultAvailableFrom = now.minus(1, ChronoUnit.MINUTES);
        Instant defaultAvailableUntil = now.plus(3650, ChronoUnit.DAYS);

        TestAssignment assignment = testAssignmentRepository
                .findFirstByTestIdAndCourseLectureId(testId, lectureId)
                .orElseGet(() -> {
                    TestAssignment created = new TestAssignment();
                    created.setTestId(testId);
                    created.setScope(3);
                    created.setCourseLectureId(lectureId);
                    created.setAvailableFromUtc(defaultAvailableFrom);
                    created.setAvailableUntilUtc(defaultAvailableUntil);
                    created.setStatus(2);
                    return created;
                });

        boolean changed = assignment.getId() == null;
        if (assignment.getScope() != 3) {
            assignment.setScope(3);
            changed = true;
        }
        if (!Objects.equals(assignment.getCourseLectureId(), lectureId)) {
            assignment.setCourseLectureId(lectureId);
            changed = true;
        }
        if (assignment.getCourseVersionId() != null) {
            assignment.setCourseVersionId(null);
            changed = true;
        }
        if (assignment.getTeachingAssignmentId() != null) {
            assignment.setTeachingAssignmentId(null);
            changed = true;
        }
        if (assignment.getAvailableFromUtc() == null || now.isBefore(assignment.getAvailableFromUtc())) {
            assignment.setAvailableFromUtc(defaultAvailableFrom);
            changed = true;
        }
        if (assignment.getAvailableUntilUtc() == null || !now.isBefore(assignment.getAvailableUntilUtc())) {
            assignment.setAvailableUntilUtc(defaultAvailableUntil);
            changed = true;
        }
        if (assignment.getStatus() != 2) {
            assignment.setStatus(2);
            changed = true;
        }

        return changed ? testAssignmentRepository.save(assignment) : assignment;
    }

    private Lecture requireLecture(Integer lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found: " + lectureId));
    }
}
