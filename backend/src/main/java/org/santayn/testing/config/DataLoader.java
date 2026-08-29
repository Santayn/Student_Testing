package org.santayn.testing.config;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.course.CourseTemplate;
import org.santayn.testing.models.course.CourseVersion;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionOption;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.role.Permission;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.santayn.testing.models.teacher.TeachingAssignmentEnrollment;
import org.santayn.testing.models.teacher.TeachingLoadType;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.FacultySubjectRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.PermissionRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.RoleRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingLoadTypeRepository;
import org.santayn.testing.repository.UserRepository;
import org.santayn.testing.service.CourseService;
import org.santayn.testing.service.FacultySubjectService;
import org.santayn.testing.service.LectureService;
import org.santayn.testing.service.LectureTestLinkService;
import org.santayn.testing.service.MembershipService;
import org.santayn.testing.service.QuestionService;
import org.santayn.testing.service.TeachingService;
import org.santayn.testing.service.TestService;
import org.santayn.testing.service.TopicService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data-loader.enabled", havingValue = "true")
public class DataLoader implements CommandLineRunner {

    private static final int MEMBERSHIP_ROLE_TEACHER = 1;
    private static final int MEMBERSHIP_ROLE_STUDENT = 1;
    private static final int ACTIVE_STATUS = 1;

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final FacultyRepository facultyRepository;
    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final FacultySubjectRepository facultySubjectRepository;
    private final TeachingLoadTypeRepository teachingLoadTypeRepository;
    private final QuestionRepository questionRepository;
    private final MembershipService membershipService;
    private final FacultySubjectService facultySubjectService;
    private final CourseService courseService;
    private final LectureService lectureService;
    private final LectureTestLinkService lectureTestLinkService;
    private final TopicService topicService;
    private final QuestionService questionService;
    private final TestService testService;
    private final TeachingService teachingService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        createRoleIfNotExists("USER", "Default authenticated user");
        createRoleIfNotExists("STUDENT", "Student");
        createRoleIfNotExists("TEACHER", "Teacher");
        createRoleIfNotExists("ADMIN", "Administrator");

        createPermissionIfNotExists("users.read", "Read users");
        createPermissionIfNotExists("users.write", "Manage users");
        createPermissionIfNotExists("roles.manage", "Manage roles and permissions");
        createPermissionIfNotExists("people.read", "Read people");
        createPermissionIfNotExists("people.write", "Manage people");
        createPermissionIfNotExists("academic.manage", "Manage faculties, groups, subjects and memberships");
        createPermissionIfNotExists("courses.manage", "Manage course templates, versions and lectures");
        createPermissionIfNotExists("teaching.manage", "Manage teaching assignments and enrollments");
        createPermissionIfNotExists("tests.manage", "Manage tests and assignments");
        createPermissionIfNotExists("questions.manage", "Manage test questions and options");
        createPermissionIfNotExists("tests.take", "Take assigned tests");
        createPermissionIfNotExists("lectures.read", "Read assigned lectures");

        createTeachingLoadTypeIfNotExists("Lecture", "Lecture classes");
        createTeachingLoadTypeIfNotExists("Practice", "Practice classes");
        createTeachingLoadTypeIfNotExists("Laboratory", "Laboratory classes");

        assignPermissions("ADMIN",
                "users.read",
                "users.write",
                "roles.manage",
                "people.read",
                "people.write",
                "academic.manage",
                "courses.manage",
                "teaching.manage",
                "tests.manage",
                "questions.manage",
                "tests.take",
                "lectures.read"
        );
        assignPermissions("TEACHER",
                "people.read",
                "people.write",
                "academic.manage",
                "courses.manage",
                "teaching.manage",
                "tests.manage",
                "questions.manage",
                "lectures.read"
        );
        assignPermissions("STUDENT", "tests.take", "lectures.read");

        createUserIfNotExists("student", "student1", "Student", "User", "student@example.local", "+10000000001", "STUDENT");
        createUserIfNotExists("student2", "student2", "Student", "Second", "student2@example.local", "+10000000004", "STUDENT");
        createUserIfNotExists("student3", "student3", "Student", "Third", "student3@example.local", "+10000000006", "STUDENT");
        createUserIfNotExists("teacher", "teacher1", "Teacher", "User", "teacher@example.local", "+10000000002", "TEACHER");
        createUserIfNotExists("teacher2", "teacher2", "Teacher", "Second", "teacher2@example.local", "+10000000005", "TEACHER");
        createUserIfNotExists("teacher3", "teacher3", "Teacher", "Third", "teacher3@example.local", "+10000000007", "TEACHER");
        createUserIfNotExists("admin", "admin123", "Admin", "User", "admin@example.local", "+10000000003", "ADMIN");

        seedAcademicDefaults();
    }

    private void createRoleIfNotExists(String roleName, String description) {
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        if (roleRepository.findByName(normalized).isEmpty()) {
            Role role = new Role();
            role.setName(normalized);
            role.setDescription(description);
            roleRepository.save(role);
        }
    }

    private void createPermissionIfNotExists(String permissionName, String description) {
        String normalized = permissionName.trim().toLowerCase(Locale.ROOT);
        if (permissionRepository.findByName(normalized).isEmpty()) {
            Permission permission = new Permission();
            permission.setName(normalized);
            permission.setDescription(description);
            permissionRepository.save(permission);
        }
    }

    private void createTeachingLoadTypeIfNotExists(String name, String description) {
        String normalized = name.trim();
        if (teachingLoadTypeRepository.findByName(normalized).isEmpty()) {
            TeachingLoadType loadType = new TeachingLoadType();
            loadType.setName(normalized);
            loadType.setDescription(description);
            teachingLoadTypeRepository.save(loadType);
        }
    }

    private void seedAcademicDefaults() {
        Faculty softwareEngineering = createFacultyIfNotExists(
                "PI",
                "Software Engineering",
                "Seed faculty for core academic flows."
        );
        Faculty informationSystems = createFacultyIfNotExists(
                "IS",
                "Information Systems",
                "Seed faculty with a separate group and subject mapping."
        );
        Faculty appliedAi = createFacultyIfNotExists(
                "AI",
                "Applied AI",
                "Seed faculty for a third academic branch."
        );

        Group group201 = createGroupIfNotExists("PI-201", "PI-201", softwareEngineering.getId());
        Group group202 = createGroupIfNotExists("PI-202", "PI-202", softwareEngineering.getId());
        Group group301 = createGroupIfNotExists("IS-301", "IS-301", informationSystems.getId());
        createGroupIfNotExists("AI-401", "AI-401", appliedAi.getId());

        Subject informatics = createSubjectIfNotExists(
                "Informatics",
                "Seed subject for introductory computer science content."
        );
        Subject databases = createSubjectIfNotExists(
                "Databases",
                "Seed subject for relational database content."
        );
        Subject algorithms = createSubjectIfNotExists(
                "Algorithms",
                "Seed subject for algorithmic thinking and complexity."
        );

        ensureFacultySubject(softwareEngineering.getId(), informatics.getId());
        ensureFacultySubject(softwareEngineering.getId(), databases.getId());
        ensureFacultySubject(informationSystems.getId(), databases.getId());
        ensureFacultySubject(informationSystems.getId(), algorithms.getId());
        ensureFacultySubject(appliedAi.getId(), informatics.getId());
        ensureFacultySubject(appliedAi.getId(), algorithms.getId());

        User teacher1 = requireUser("teacher");
        User teacher2 = requireUser("teacher2");
        User teacher3 = requireUser("teacher3");
        User student1 = requireUser("student");
        User student2 = requireUser("student2");
        User student3 = requireUser("student3");

        SubjectMembership teacherInformatics = ensureSubjectMembership(
                informatics.getId(),
                teacher1.getPersonId(),
                MEMBERSHIP_ROLE_TEACHER,
                "Seed teacher assignment for Informatics."
        );
        SubjectMembership teacherDatabases = ensureSubjectMembership(
                databases.getId(),
                teacher1.getPersonId(),
                MEMBERSHIP_ROLE_TEACHER,
                "Seed teacher assignment for Databases."
        );
        SubjectMembership teacher2Informatics = ensureSubjectMembership(
                informatics.getId(),
                teacher2.getPersonId(),
                MEMBERSHIP_ROLE_TEACHER,
                "Seed secondary teacher assignment for Informatics."
        );
        SubjectMembership teacherAlgorithms = ensureSubjectMembership(
                algorithms.getId(),
                teacher3.getPersonId(),
                MEMBERSHIP_ROLE_TEACHER,
                "Seed teacher assignment for Algorithms."
        );

        GroupMembership studentGroup201 = ensureGroupMembership(
                group201.getId(),
                student1.getPersonId(),
                MEMBERSHIP_ROLE_STUDENT,
                "Seed group membership for PI-201."
        );
        GroupMembership studentGroup202 = ensureGroupMembership(
                group202.getId(),
                student2.getPersonId(),
                MEMBERSHIP_ROLE_STUDENT,
                "Seed group membership for PI-202."
        );
        GroupMembership studentGroup301 = ensureGroupMembership(
                group301.getId(),
                student3.getPersonId(),
                MEMBERSHIP_ROLE_STUDENT,
                "Seed group membership for IS-301."
        );

        TeachingLoadType defaultLoadType = ensureTeachingLoadType(
                "Main workload",
                "System load type for seeded assignments."
        );

        ensureSubjectLoadType(teacherInformatics.getId(), defaultLoadType.getId());
        ensureSubjectLoadType(teacherDatabases.getId(), defaultLoadType.getId());
        ensureSubjectLoadType(teacher2Informatics.getId(), defaultLoadType.getId());
        ensureSubjectLoadType(teacherAlgorithms.getId(), defaultLoadType.getId());

        seedTeachingFlow(
                informatics,
                teacherInformatics,
                studentGroup201,
                defaultLoadType,
                "Seed Informatics Template",
                "Seed Informatics Version",
                "Seed Informatics Lecture",
                "seed-informatics-lecture",
                "Seed Informatics Topic",
                "Seed Informatics Test",
                "Seed informatics assessment.",
                "What does CPU stand for?",
                "Central Processing Unit",
                "Which device stores data permanently?",
                "RAM",
                "SSD",
                "CPU",
                group201.getId(),
                1,
                1,
                2026,
                "2.00"
        );

        seedTeachingFlow(
                databases,
                teacherDatabases,
                studentGroup202,
                defaultLoadType,
                "Seed Databases Template",
                "Seed Databases Version",
                "Seed Databases Lecture",
                "seed-databases-lecture",
                "Seed Databases Topic",
                "Seed Databases Test",
                "Seed databases assessment.",
                "What does SQL stand for?",
                "Structured Query Language",
                "Which command reads rows from a table?",
                "INSERT",
                "SELECT",
                "DELETE",
                group202.getId(),
                1,
                2,
                2026,
                "2.50"
        );

        seedTeachingFlow(
                algorithms,
                teacherAlgorithms,
                studentGroup301,
                defaultLoadType,
                "Seed Algorithms Template",
                "Seed Algorithms Version",
                "Seed Algorithms Lecture",
                "seed-algorithms-lecture",
                "Seed Algorithms Topic",
                "Seed Algorithms Test",
                "Seed algorithms assessment.",
                "What is the complexity of binary search?",
                "O(log n)",
                "Which structure follows FIFO?",
                "Stack",
                "Queue",
                "Tree",
                group301.getId(),
                2,
                1,
                2026,
                "3.00"
        );
    }

    private void seedTeachingFlow(Subject subject,
                                  SubjectMembership subjectMembership,
                                  GroupMembership studentMembership,
                                  TeachingLoadType loadType,
                                  String templateName,
                                  String versionTitle,
                                  String lectureTitle,
                                  String contentFolderKey,
                                  String topicName,
                                  String testTitle,
                                  String testDescription,
                                  String textQuestionText,
                                  String textCorrectAnswer,
                                  String singleQuestionText,
                                  String wrongOptionOne,
                                  String correctOption,
                                  String wrongOptionTwo,
                                  Integer groupId,
                                  int studyCourse,
                                  int semester,
                                  int academicYear,
                                  String hoursPerWeek) {
        CourseTemplate template = ensureCourseTemplate(
                subject.getId(),
                subjectMembership.getPersonId(),
                templateName,
                true
        );
        CourseVersion version = ensureCourseVersion(
                template.getId(),
                1,
                versionTitle,
                testDescription,
                subjectMembership.getPersonId(),
                "Seed published version."
        );
        Lecture lecture = ensureLecture(
                subject.getId(),
                subjectMembership.getId(),
                version.getId(),
                101,
                lectureTitle,
                testDescription,
                contentFolderKey
        );
        Topic topic = ensureTopic(
                subject.getId(),
                lecture.getId(),
                subjectMembership.getId(),
                101,
                topicName,
                "Seed topic for question selection rules."
        );

        Question singleAnswerQuestion = ensureTopicQuestion(
                lecture.getId(),
                topic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                101,
                singleQuestionText,
                null,
                List.of()
        );
        ensureQuestionOption(singleAnswerQuestion.getId(), 1, wrongOptionOne, false);
        ensureQuestionOption(singleAnswerQuestion.getId(), 2, correctOption, true);
        ensureQuestionOption(singleAnswerQuestion.getId(), 3, wrongOptionTwo, false);

        Question multipleAnswerQuestion = ensureTopicQuestion(
                lecture.getId(),
                topic.getId(),
                QuestionTypeSupport.TYPE_MULTIPLE,
                102,
                "Выберите два верных утверждения по теме «" + topicName + "».",
                null,
                List.of()
        );
        ensureQuestionOption(multipleAnswerQuestion.getId(), 1, "Материал темы относится к выбранному предмету.", true);
        ensureQuestionOption(multipleAnswerQuestion.getId(), 2, "Тест проверяет знания по материалу темы.", true);
        ensureQuestionOption(multipleAnswerQuestion.getId(), 3, "Тема не связана с лекцией курса.", false);
        ensureQuestionOption(multipleAnswerQuestion.getId(), 4, "Любой ответ в тесте всегда считается верным.", false);

        ensureTopicQuestion(
                lecture.getId(),
                topic.getId(),
                QuestionTypeSupport.TYPE_MATCHING,
                103,
                "Соотнесите элементы колонки А с элементами колонки Б по теме «" + topicName + "».",
                null,
                List.of(
                        new QuestionTypeSupport.MatchingPair(1, "Лекция", "Учебный материал"),
                        new QuestionTypeSupport.MatchingPair(2, "Тема", "Раздел лекции"),
                        new QuestionTypeSupport.MatchingPair(3, "Тест", "Проверка знаний")
                )
        );

        ensureTopicQuestion(
                lecture.getId(),
                topic.getId(),
                QuestionTypeSupport.TYPE_TEXT,
                104,
                textQuestionText,
                textCorrectAnswer,
                List.of()
        );

        Test test = ensureTest(
                testTitle,
                testDescription,
                LocalTime.of(0, 20),
                2,
                4,
                List.of(new TestService.SelectionRuleInput(
                        null,
                        topic.getId(),
                        4,
                        1,
                        1,
                        1,
                        1,
                        1
                ))
        );

        lectureService.updateLinkedTest(lecture.getId(), test.getId());
        lectureTestLinkService.replaceLectureTests(lecture.getId(), List.of(test.getId()));

        TeachingAssignment assignment = ensureTeachingAssignment(
                subjectMembership.getId(),
                groupId,
                loadType.getId(),
                version.getId(),
                semester,
                studyCourse,
                academicYear,
                new BigDecimal(hoursPerWeek),
                "Seed teaching assignment."
        );
        ensureTeachingAssignmentEnrollment(assignment.getId(), studentMembership.getId());
        ensureLectureAssignment(assignment.getId(), lecture.getId());
    }

    private Faculty createFacultyIfNotExists(String code, String name, String description) {
        return facultyRepository.findByCode(code)
                .orElseGet(() -> {
                    Faculty faculty = new Faculty();
                    faculty.setCode(code);
                    faculty.setName(name);
                    faculty.setDescription(description);
                    return facultyRepository.save(faculty);
                });
    }

    private Group createGroupIfNotExists(String code, String name, Integer facultyId) {
        return groupRepository.findByCode(code)
                .orElseGet(() -> {
                    Group group = new Group();
                    group.setCode(code);
                    group.setName(name);
                    group.setFacultyId(facultyId);
                    return groupRepository.save(group);
                });
    }

    private Subject createSubjectIfNotExists(String name, String description) {
        return subjectRepository.findByName(name)
                .orElseGet(() -> {
                    Subject subject = new Subject();
                    subject.setName(name);
                    subject.setDescription(description);
                    return subjectRepository.save(subject);
                });
    }

    private void ensureFacultySubject(Integer facultyId, Integer subjectId) {
        if (!facultySubjectRepository.existsByFacultyIdAndSubjectId(facultyId, subjectId)) {
            facultySubjectService.link(facultyId, subjectId);
        }
    }

    private SubjectMembership ensureSubjectMembership(Integer subjectId, Integer personId, int role, String notes) {
        return membershipService.subjectMembers(subjectId, personId, null, true)
                .stream()
                .filter(membership -> membership.getRole() == role)
                .findFirst()
                .orElseGet(() -> membershipService.addSubjectMember(subjectId, personId, role, notes));
    }

    private GroupMembership ensureGroupMembership(Integer groupId, Integer personId, int role, String notes) {
        return membershipService.groupMembers(groupId, personId, null, true)
                .stream()
                .filter(membership -> membership.getRole() == role)
                .findFirst()
                .orElseGet(() -> membershipService.addGroupMember(groupId, personId, role, notes));
    }

    private TeachingLoadType ensureTeachingLoadType(String name, String description) {
        return teachingLoadTypeRepository.findByName(name)
                .orElseGet(() -> {
                    TeachingLoadType loadType = new TeachingLoadType();
                    loadType.setName(name);
                    loadType.setDescription(description);
                    return teachingLoadTypeRepository.save(loadType);
                });
    }

    private void ensureSubjectLoadType(Integer subjectMembershipId, Integer loadTypeId) {
        teachingService.addSubjectLoadType(
                subjectMembershipId,
                loadTypeId,
                "Seed allowed load type for subject membership."
        );
    }

    private CourseTemplate ensureCourseTemplate(Integer subjectId,
                                                Integer authorPersonId,
                                                String name,
                                                boolean publicVisible) {
        return courseService.findTemplates(subjectId, authorPersonId, false)
                .stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseGet(() -> courseService.createTemplate(subjectId, authorPersonId, name, publicVisible));
    }

    private CourseVersion ensureCourseVersion(Integer courseTemplateId,
                                              int versionNumber,
                                              String title,
                                              String description,
                                              Integer createdByPersonId,
                                              String changeNotes) {
        CourseVersion version = courseService.findVersions(courseTemplateId)
                .stream()
                .filter(item -> item.getVersionNumber() == versionNumber)
                .findFirst()
                .orElseGet(() -> courseService.createVersion(
                        courseTemplateId,
                        versionNumber,
                        title,
                        description,
                        createdByPersonId,
                        true,
                        createdByPersonId,
                        changeNotes
                ));

        if (!version.isPublished()) {
            version = courseService.publishVersion(version.getId(), createdByPersonId);
        }
        return version;
    }

    private Lecture ensureLecture(Integer subjectId,
                                  Integer subjectMembershipId,
                                  Integer courseVersionId,
                                  int ordinal,
                                  String title,
                                  String description,
                                  String contentFolderKey) {
        return lectureService.findAll(null, subjectMembershipId, null)
                .stream()
                .filter(item -> contentFolderKey.equals(item.getContentFolderKey()))
                .findFirst()
                .orElseGet(() -> lectureService.create(
                        subjectId,
                        subjectMembershipId,
                        courseVersionId,
                        ordinal,
                        title,
                        description,
                        contentFolderKey,
                        null,
                        true
                ));
    }

    private Topic ensureTopic(Integer subjectId,
                              Integer courseLectureId,
                              Integer subjectMembershipId,
                              int ordinal,
                              String name,
                              String description) {
        return topicService.findAll(null, null, subjectMembershipId)
                .stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseGet(() -> topicService.create(
                        subjectId,
                        courseLectureId,
                        subjectMembershipId,
                        ordinal,
                        name,
                        description
                ));
    }

    private Question ensureTopicQuestion(Integer courseLectureId,
                                         Integer topicId,
                                         int type,
                                         int ordinal,
                                         String questionText,
                                         String correctAnswer,
                                         List<QuestionTypeSupport.MatchingPair> matchingPairs) {
        Question existing = questionService.findAll(null, topicId)
                .stream()
                .filter(item -> questionText.equals(item.getQuestion()))
                .findFirst()
                .orElse(null);

        freeQuestionOrdinal(topicId, ordinal, existing == null ? null : existing.getId());

        if (existing == null) {
            return questionService.create(
                    null,
                    courseLectureId,
                    topicId,
                    type,
                    questionText,
                    BigDecimal.ONE,
                    ordinal,
                    correctAnswer,
                    matchingPairs
            );
        }

        return questionService.update(
                existing.getId(),
                courseLectureId,
                topicId,
                type,
                questionText,
                existing.getPoints() == null ? BigDecimal.ONE : existing.getPoints(),
                ordinal,
                correctAnswer,
                matchingPairs,
                true
        );
    }


    private void freeQuestionOrdinal(Integer topicId, int ordinal, Long excludedQuestionId) {
        List<Question> questions = questionService.findAll(null, topicId);
        Question conflict = questions.stream()
                .filter(item -> item.getOrdinal() == ordinal)
                .filter(item -> excludedQuestionId == null || !excludedQuestionId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (conflict == null) {
            return;
        }

        int maxOrdinal = questions.stream()
                .mapToInt(Question::getOrdinal)
                .max()
                .orElse(ordinal);
        conflict.setOrdinal(maxOrdinal + 100);
        questionRepository.save(conflict);
        questionRepository.flush();
    }

    private QuestionOption ensureQuestionOption(Long questionId,
                                                int ordinal,
                                                String text,
                                                boolean correct) {
        QuestionOption existing = questionService.findOptions(questionId)
                .stream()
                .filter(option -> option.getOrdinal() == ordinal)
                .findFirst()
                .orElse(null);

        if (existing == null) {
            return questionService.addOption(questionId, text, ordinal, correct);
        }

        if (!text.equals(existing.getText()) || existing.isCorrect() != correct) {
            return questionService.updateOption(existing.getId(), text, ordinal, correct);
        }
        return existing;
    }

    private Test ensureTest(String title,
                            String description,
                            LocalTime duration,
                            int attemptsAllowed,
                            int questionCount,
                            List<TestService.SelectionRuleInput> selectionRules) {
        Test existing = testService.findAll()
                .stream()
                .filter(item -> title.equals(item.getTitle()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            return testService.create(title, description, duration, attemptsAllowed, questionCount, selectionRules);
        }

        return testService.update(
                existing.getId(),
                title,
                description,
                duration,
                attemptsAllowed,
                questionCount,
                selectionRules
        );
    }

    private TeachingAssignment ensureTeachingAssignment(Integer subjectMembershipId,
                                                        Integer groupId,
                                                        Integer loadTypeId,
                                                        Integer courseVersionId,
                                                        int semester,
                                                        Integer studyCourse,
                                                        int academicYear,
                                                        BigDecimal hoursPerWeek,
                                                        String notes) {
        return teachingService.findAssignments(
                        groupId,
                        subjectMembershipId,
                        courseVersionId,
                        null,
                        loadTypeId,
                        studyCourse,
                        semester,
                        academicYear,
                        null
                )
                .stream()
                .findFirst()
                .orElseGet(() -> teachingService.createAssignment(
                        subjectMembershipId,
                        groupId,
                        loadTypeId,
                        courseVersionId,
                        semester,
                        studyCourse,
                        academicYear,
                        hoursPerWeek,
                        ACTIVE_STATUS,
                        notes
                ));
    }

    private TeachingAssignmentEnrollment ensureTeachingAssignmentEnrollment(Integer teachingAssignmentId,
                                                                            Integer groupMembershipId) {
        return teachingService.findEnrollments(teachingAssignmentId, groupMembershipId, null, null)
                .stream()
                .findFirst()
                .orElseGet(() -> teachingService.enroll(teachingAssignmentId, groupMembershipId, ACTIVE_STATUS));
    }

    private void ensureLectureAssignment(Integer teachingAssignmentId, Integer lectureId) {
        Instant availableFromUtc = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant dueToUtc = Instant.now().plus(120, ChronoUnit.DAYS);

        if (teachingService.findLectureAssignments(teachingAssignmentId, lectureId).isEmpty()) {
            teachingService.assignLecture(
                    teachingAssignmentId,
                    lectureId,
                    availableFromUtc,
                    dueToUtc,
                    null,
                    true,
                    0,
                    ACTIVE_STATUS
            );
        }
    }

    private void assignPermissions(String roleName, String... permissionNames) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Seed role was not created: " + roleName));

        for (String permissionName : permissionNames) {
            Permission permission = permissionRepository.findByName(permissionName)
                    .orElseThrow(() -> new IllegalStateException("Seed permission was not created: " + permissionName));
            role.getPermissions().add(permission);
        }
    }

    private void createUserIfNotExists(String login,
                                       String password,
                                       String firstName,
                                       String lastName,
                                       String email,
                                       String phone,
                                       String roleName) {
        String normalizedLogin = login.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByLogin(normalizedLogin)) {
            return;
        }

        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail(email);
        person.setPhone(phone);
        person = personRepository.save(person);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Seed role was not created: " + roleName));

        User user = new User();
        user.setLogin(normalizedLogin);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setActive(true);
        user.setPersonId(person.getId());
        user.getRoles().add(role);
        userRepository.save(user);
    }

    private User requireUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalStateException("Seed user was not created: " + login));
    }
}
