package org.santayn.testing.service;

import org.santayn.testing.models.course.CourseTemplate;
import org.santayn.testing.models.course.CourseVersion;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.repository.CourseTemplateRepository;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Тесты, результаты которых видит преподаватель: свои тесты и тесты, привязанные к его лекциям
 * (лекции предметов, за которыми он закреплён, и лекции его шаблонов курсов).
 */
@Service
public class TeacherTestScopeService {

    private static final int SUBJECT_ROLE_TEACHER = 1;

    private final TestRepository testRepository;
    private final SubjectMembershipRepository subjectMembershipRepository;
    private final LectureRepository lectureRepository;
    private final CourseTemplateRepository courseTemplateRepository;
    private final CourseVersionRepository courseVersionRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final LectureTestLinkService lectureTestLinkService;

    public TeacherTestScopeService(TestRepository testRepository,
                                   SubjectMembershipRepository subjectMembershipRepository,
                                   LectureRepository lectureRepository,
                                   CourseTemplateRepository courseTemplateRepository,
                                   CourseVersionRepository courseVersionRepository,
                                   TestAssignmentRepository testAssignmentRepository,
                                   LectureTestLinkService lectureTestLinkService) {
        this.testRepository = testRepository;
        this.subjectMembershipRepository = subjectMembershipRepository;
        this.lectureRepository = lectureRepository;
        this.courseTemplateRepository = courseTemplateRepository;
        this.courseVersionRepository = courseVersionRepository;
        this.testAssignmentRepository = testAssignmentRepository;
        this.lectureTestLinkService = lectureTestLinkService;
    }

    @Transactional(readOnly = true)
    public Set<Integer> allTestIds() {
        return testRepository.findAll().stream()
                .map(Test::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public Set<Integer> teacherTestIds(Integer teacherPersonId) {
        Set<Integer> ids = testRepository.findAll().stream()
                .filter(test -> teacherPersonId.equals(test.getAuthorPersonId()))
                .map(Test::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Lecture lecture : teacherLectures(teacherPersonId).values()) {
            ids.addAll(lectureTestLinkService.findTestIdsByLectureId(lecture.getId()));
            testAssignmentRepository.findByCourseLectureId(lecture.getId())
                    .stream()
                    .map(TestAssignment::getTestId)
                    .forEach(ids::add);
            if (lecture.getLinkedTestId() != null) {
                ids.add(lecture.getLinkedTestId());
            }
        }
        return ids;
    }

    private Map<Integer, Lecture> teacherLectures(Integer teacherPersonId) {
        Map<Integer, Lecture> lecturesById = new LinkedHashMap<>();
        for (SubjectMembership membership : subjectMembershipRepository.findByPersonIdAndRemovedAtUtcIsNull(teacherPersonId)) {
            if (membership.getRole() != SUBJECT_ROLE_TEACHER) {
                continue;
            }
            lectureRepository.findBySubjectMembershipIdOrderByOrdinalAsc(membership.getId())
                    .forEach(lecture -> lecturesById.put(lecture.getId(), lecture));
            for (CourseTemplate template : courseTemplateRepository.findByFilters(
                    membership.getSubjectId(), teacherPersonId, false)) {
                for (CourseVersion version : courseVersionRepository.findByCourseTemplateIdOrderByVersionNumberDesc(template.getId())) {
                    lectureRepository.findByCourseVersionIdOrderByOrdinalAsc(version.getId())
                            .forEach(lecture -> lecturesById.putIfAbsent(lecture.getId(), lecture));
                }
            }
        }
        return lecturesById;
    }
}
