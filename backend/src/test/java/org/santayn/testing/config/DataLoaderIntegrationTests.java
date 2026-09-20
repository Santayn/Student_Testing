package org.santayn.testing.config;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.course.CourseTemplate;
import org.santayn.testing.models.course.CourseVersion;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.santayn.testing.models.teacher.TeachingAssignmentEnrollment;
import org.santayn.testing.models.teacher.TeachingLoadType;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestQuestionSelectionRule;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.CourseTemplateRepository;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.FacultySubjectRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.LectureAssignmentRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingAssignmentEnrollmentRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TeachingLoadTypeRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.TopicRepository;
import org.santayn.testing.repository.UserRepository;
import org.santayn.testing.service.LectureTestLinkService;
import org.santayn.testing.service.MembershipService;
import org.santayn.testing.service.TeachingService;
import org.santayn.testing.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.data-loader.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:data_loader_relationships;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataLoaderIntegrationTests {

    @Autowired private CourseTemplateRepository courseTemplateRepository;
    @Autowired private CourseVersionRepository courseVersionRepository;
    @Autowired private DataLoader dataLoader;
    @Autowired private FacultySubjectRepository facultySubjectRepository;
    @Autowired private GroupMembershipRepository groupMembershipRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private LectureAssignmentRepository lectureAssignmentRepository;
    @Autowired private LectureRepository lectureRepository;
    @Autowired private LectureTestLinkService lectureTestLinkService;
    @Autowired private MembershipService membershipService;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private TeachingAssignmentEnrollmentRepository teachingAssignmentEnrollmentRepository;
    @Autowired private TeachingAssignmentRepository teachingAssignmentRepository;
    @Autowired private TeachingLoadTypeRepository teachingLoadTypeRepository;
    @Autowired private TeachingService teachingService;
    @Autowired private TestAssignmentRepository testAssignmentRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TestService testService;
    @Autowired private TopicRepository topicRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void seedsConsistentSubjectToTestChains() {
        assertSeedChain(new SeedChain(
                "Informatics",
                "PI-201",
                "teacher",
                "student",
                "Seed Informatics Template",
                "Seed Informatics Version",
                "seed-informatics-lecture",
                "Seed Informatics Topic",
                "Seed Informatics Test",
                1,
                1,
                2026,
                "2.00"
        ));
        assertSeedChain(new SeedChain(
                "Databases",
                "PI-202",
                "teacher2",
                "student2",
                "Seed Databases Template",
                "Seed Databases Version",
                "seed-databases-lecture",
                "Seed Databases Topic",
                "Seed Databases Test",
                1,
                2,
                2026,
                "2.50"
        ));
        assertSeedChain(new SeedChain(
                "Algorithms",
                "IS-301",
                "teacher3",
                "student3",
                "Seed Algorithms Template",
                "Seed Algorithms Version",
                "seed-algorithms-lecture",
                "Seed Algorithms Topic",
                "Seed Algorithms Test",
                2,
                1,
                2026,
                "3.00"
        ));
    }

    @Test
    void seededTestsAreVisibleOnlyThroughTheirOwnSubjects() {
        assertSubjectTests("Informatics", List.of("Seed Informatics Test"));
        assertSubjectTests("Databases", List.of("Seed Databases Test"));
        assertSubjectTests("Algorithms", List.of("Seed Algorithms Test"));
    }

    @Test
    void seededTeachersHaveOneActiveSubjectEach() {
        assertActiveTeacherSubjects("teacher", List.of("Informatics"));
        assertActiveTeacherSubjects("teacher2", List.of("Databases"));
        assertActiveTeacherSubjects("teacher3", List.of("Algorithms"));
    }

    @Test
    void rerunRetiresObsoleteSeedTeacherSubjectMemberships() {
        Subject informatics = subjectRepository.findByName("Informatics").orElseThrow();
        Subject databases = subjectRepository.findByName("Databases").orElseThrow();
        User teacher = userRepository.findByLogin("teacher").orElseThrow();
        User teacher2 = userRepository.findByLogin("teacher2").orElseThrow();

        SubjectMembership obsoleteTeacherDatabases = membershipService.addSubjectMember(
                databases.getId(),
                teacher.getPersonId(),
                1,
                "Seed teacher assignment for Databases."
        );
        SubjectMembership obsoleteTeacher2Informatics = membershipService.addSubjectMember(
                informatics.getId(),
                teacher2.getPersonId(),
                1,
                "Seed secondary teacher assignment for Informatics."
        );

        dataLoader.run();

        assertThat(membershipService.getSubjectMembership(obsoleteTeacherDatabases.getId()).getRemovedAtUtc()).isNotNull();
        assertThat(membershipService.getSubjectMembership(obsoleteTeacher2Informatics.getId()).getRemovedAtUtc()).isNotNull();
        assertActiveTeacherSubjects("teacher", List.of("Informatics"));
        assertActiveTeacherSubjects("teacher2", List.of("Databases"));
        assertActiveTeacherSubjects("teacher3", List.of("Algorithms"));
    }

    private void assertSeedChain(SeedChain expected) {
        Subject subject = subjectRepository.findByName(expected.subjectName()).orElseThrow();
        Group group = groupRepository.findByCode(expected.groupCode()).orElseThrow();
        User teacher = userRepository.findByLogin(expected.teacherLogin()).orElseThrow();
        User student = userRepository.findByLogin(expected.studentLogin()).orElseThrow();
        TeachingLoadType loadType = teachingLoadTypeRepository.findByName("Main workload").orElseThrow();

        assertThat(facultySubjectRepository.existsByFacultyIdAndSubjectId(group.getFacultyId(), subject.getId()))
                .as("group faculty is linked to subject")
                .isTrue();

        SubjectMembership subjectMembership = singleItem(membershipService.subjectMembers(
                subject.getId(),
                teacher.getPersonId(),
                null,
                true
        ));
        assertThat(subjectMembership.getRole()).isEqualTo(1);
        assertThat(subjectMembership.getStatus()).isEqualTo(1);

        assertThat(teachingService.findSubjectLoadTypes(subjectMembership.getId(), loadType.getId()))
                .as("teacher subject membership load type")
                .hasSize(1)
                .allSatisfy(item -> assertThat(item.getStatus()).isEqualTo(1));

        GroupMembership groupMembership = singleItem(groupMembershipRepository.findByFilters(
                group.getId(),
                student.getPersonId(),
                null,
                true
        ));
        assertThat(groupMembership.getRole()).isEqualTo(1);
        assertThat(groupMembership.getStatus()).isEqualTo(1);

        CourseTemplate template = singleItem(courseTemplateRepository.findByFilters(
                subject.getId(),
                subjectMembership.getPersonId(),
                false
        ).stream()
                .filter(item -> expected.templateName().equals(item.getName()))
                .toList());
        assertThat(template.isPublicVisible()).isTrue();

        CourseVersion version = singleItem(courseVersionRepository.findByCourseTemplateIdOrderByVersionNumberDesc(template.getId()));
        assertThat(version.getTitle()).isEqualTo(expected.versionTitle());
        assertThat(version.getCreatedByPersonId()).isEqualTo(subjectMembership.getPersonId());
        assertThat(version.isPublished()).isTrue();
        assertThat(version.getPublishedByPersonId()).isEqualTo(subjectMembership.getPersonId());

        Lecture lecture = singleItem(lectureRepository.findBySubjectMembershipIdOrderByOrdinalAsc(subjectMembership.getId())
                .stream()
                .filter(item -> expected.contentFolderKey().equals(item.getContentFolderKey()))
                .toList());
        assertThat(lecture.getSubjectId()).isEqualTo(subject.getId());
        assertThat(lecture.getSubjectMembershipId()).isEqualTo(subjectMembership.getId());
        assertThat(lecture.getCourseVersionId()).isEqualTo(version.getId());
        assertThat(lecture.isPublicVisible()).isTrue();

        Topic topic = singleItem(topicRepository.findByCourseLectureIdOrderByOrdinalAsc(lecture.getId())
                .stream()
                .filter(item -> expected.topicName().equals(item.getName()))
                .toList());
        assertThat(topic.getSubjectId()).isEqualTo(subject.getId());
        assertThat(topic.getSubjectMembershipId()).isEqualTo(subjectMembership.getId());
        assertThat(topic.getCourseLectureId()).isEqualTo(lecture.getId());

        org.santayn.testing.models.test.Test test = seededTest(expected.testTitle());
        assertThat(testRepository.existsInSubject(test.getId(), subject.getId())).isTrue();
        assertThat(lecture.getLinkedTestId()).isEqualTo(test.getId());
        assertThat(lectureTestLinkService.findTestIdsByLectureId(lecture.getId())).containsExactly(test.getId());

        TestQuestionSelectionRule rule = singleItem(testService.findSelectionRules(test.getId()));
        assertThat(rule.getCourseLectureId()).isEqualTo(lecture.getId());
        assertThat(rule.getTopicId()).isEqualTo(topic.getId());
        assertThat(rule.getQuestionCount()).isEqualTo(4);
        assertThat(rule.getSingleAnswerQuestionCount()).isEqualTo(1);
        assertThat(rule.getMultipleAnswerQuestionCount()).isEqualTo(1);
        assertThat(rule.getMatchingQuestionCount()).isEqualTo(1);
        assertThat(rule.getTextQuestionCount()).isEqualTo(1);

        assertThat(questionRepository.findByTopicIdAndTestIdIsNullAndActiveTrueOrderByOrdinalAsc(topic.getId()))
                .hasSize(4);
        assertThat(questionTypesByCount(topic))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        QuestionTypeSupport.TYPE_SINGLE, 1L,
                        QuestionTypeSupport.TYPE_MULTIPLE, 1L,
                        QuestionTypeSupport.TYPE_MATCHING, 1L,
                        QuestionTypeSupport.TYPE_TEXT, 1L
                ));
        assertThat(testService.randomQuestionsForTest(test.getId())).hasSize(4);

        TeachingAssignment assignment = singleItem(teachingAssignmentRepository.findByFilters(
                group.getId(),
                subjectMembership.getId(),
                version.getId(),
                null,
                loadType.getId(),
                expected.studyCourse(),
                expected.semester(),
                expected.academicYear(),
                1
        ));
        assertThat(assignment.getHoursPerWeek()).isEqualByComparingTo(new BigDecimal(expected.hoursPerWeek()));

        TeachingAssignmentEnrollment enrollment = singleItem(teachingAssignmentEnrollmentRepository.findByFilters(
                assignment.getId(),
                groupMembership.getId(),
                group.getId(),
                1
        ));
        assertThat(enrollment.getRemovedAtUtc()).isNull();

        assertThat(lectureAssignmentRepository.findByTeachingAssignmentIdAndCourseLectureId(
                assignment.getId(),
                lecture.getId()
        )).isPresent();

        TestAssignment lectureTestAssignment = testAssignmentRepository
                .findFirstByTestIdAndCourseLectureId(test.getId(), lecture.getId())
                .orElseThrow();
        assertThat(lectureTestAssignment.getScope()).isEqualTo(3);
        assertThat(lectureTestAssignment.getStatus()).isEqualTo(2);
    }

    private void assertSubjectTests(String subjectName, List<String> testTitles) {
        Subject subject = subjectRepository.findByName(subjectName).orElseThrow();
        assertThat(testService.findAll(subject.getId()))
                .extracting(org.santayn.testing.models.test.Test::getTitle)
                .containsExactlyElementsOf(testTitles);
    }

    private void assertActiveTeacherSubjects(String teacherLogin, List<String> subjectNames) {
        User teacher = userRepository.findByLogin(teacherLogin).orElseThrow();
        assertThat(membershipService.subjectMembers(null, teacher.getPersonId(), null, true))
                .filteredOn(membership -> membership.getRole() == 1)
                .extracting(membership -> subjectRepository.findById(membership.getSubjectId()).orElseThrow().getName())
                .containsExactlyInAnyOrderElementsOf(subjectNames);
    }

    private org.santayn.testing.models.test.Test seededTest(String title) {
        return singleItem(testRepository.findAll()
                .stream()
                .filter(item -> title.equals(item.getTitle()))
                .toList());
    }

    private Map<Integer, Long> questionTypesByCount(Topic topic) {
        return questionRepository.findByTopicIdAndTestIdIsNullAndActiveTrueOrderByOrdinalAsc(topic.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        Question::getType,
                        Collectors.counting()
                ));
    }

    private static <T> T singleItem(List<T> items) {
        assertThat(items).hasSize(1);
        return items.get(0);
    }

    private record SeedChain(String subjectName,
                             String groupCode,
                             String teacherLogin,
                             String studentLogin,
                             String templateName,
                             String versionTitle,
                             String contentFolderKey,
                             String topicName,
                             String testTitle,
                             int studyCourse,
                             int semester,
                             int academicYear,
                             String hoursPerWeek) {
    }
}
