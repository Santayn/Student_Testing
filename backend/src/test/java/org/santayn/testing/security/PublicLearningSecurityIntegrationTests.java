package org.santayn.testing.security;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.santayn.testing.models.teacher.TeachingLoadType;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TeachingLoadTypeRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicLearningSecurityIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private PersonRepository personRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMembershipRepository groupMembershipRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private SubjectMembershipRepository subjectMembershipRepository;
    @Autowired private TeachingLoadTypeRepository teachingLoadTypeRepository;
    @Autowired private TeachingAssignmentRepository teachingAssignmentRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TestAssignmentRepository testAssignmentRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionResponseRepository questionResponseRepository;

    @Test
    void getTestMetadataDoesNotCreateOrResumeAttempt() throws Exception {
        String suffix = uniqueSuffix();
        Person student = person("Student", suffix);
        saveUser("student-" + suffix, student);
        TeachingAssignment teachingAssignment = teachingAssignmentFor(student, suffix);
        org.santayn.testing.models.test.Test test = test(2, suffix);
        TestAssignment assignment = assignment(test, teachingAssignment.getId());

        mockMvc.perform(get("/api/v1/public/learning/tests/{testId}", test.getId())
                        .with(user("student-" + suffix).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentId").value(assignment.getId()))
                .andExpect(jsonPath("$.attemptsRemaining").value(2))
                .andExpect(jsonPath("$.canResume").value(false));

        assertThat(testAttemptRepository.count()).isZero();
    }

    @Test
    void studentCannotSubmitOrCompleteForeignAttempt() throws Exception {
        String suffix = uniqueSuffix();
        Person currentStudent = person("Current", suffix);
        saveUser("student-" + suffix, currentStudent);
        Person foreignStudent = person("Foreign", suffix);
        org.santayn.testing.models.test.Test test = test(1, suffix);
        TestAssignment assignment = assignment(test, null);
        TestAttempt foreignAttempt = new TestAttempt();
        foreignAttempt.setTestAssignmentId(assignment.getId());
        foreignAttempt.setPersonId(foreignStudent.getId());
        foreignAttempt.setOrdinal(1);
        foreignAttempt.setStatus(1);
        foreignAttempt = testAttemptRepository.saveAndFlush(foreignAttempt);

        mockMvc.perform(post("/api/v1/public/learning/attempts/{attemptId}/submit", foreignAttempt.getId())
                        .with(user("student-" + suffix).roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        assertThat(testAttemptRepository.findById(foreignAttempt.getId()).orElseThrow().getStatus())
                .isEqualTo(1);
    }

    @Test
    void studentResultDoesNotExposeCorrectAnswer() throws Exception {
        String suffix = uniqueSuffix();
        Person student = person("Result", suffix);
        saveUser("student-" + suffix, student);
        org.santayn.testing.models.test.Test test = test(1, suffix);
        TestAssignment assignment = assignment(test, null);

        TestAttempt attempt = new TestAttempt();
        attempt.setTestAssignmentId(assignment.getId());
        attempt.setPersonId(student.getId());
        attempt.setOrdinal(1);
        attempt.setStatus(2);
        attempt.setCompletedAt(Instant.now());
        attempt = testAttemptRepository.saveAndFlush(attempt);

        Question question = new Question();
        question.setTestId(test.getId());
        question.setType(QuestionTypeSupport.TYPE_TEXT);
        question.setQuestion("Secret question");
        question.setCorrectAnswer("SERVER-ONLY-ANSWER");
        question.setPoints(BigDecimal.ONE);
        question.setOrdinal(1);
        question.setActive(true);
        question = questionRepository.saveAndFlush(question);

        QuestionResponse response = new QuestionResponse();
        response.setTestAttemptId(attempt.getId());
        response.setTestQuestionId(question.getId());
        response.setAnswerText("student answer");
        response.setCorrect(false);
        response.setAwardedPoints(BigDecimal.ZERO);
        questionResponseRepository.saveAndFlush(response);

        mockMvc.perform(get("/api/v1/results/student/data")
                        .with(user("student-" + suffix).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempts[0].results[0].questionText").value("Secret question"))
                .andExpect(jsonPath("$.attempts[0].results[0].correctAnswer").doesNotExist());
    }

    private Person person(String name, String suffix) {
        Person person = new Person();
        person.setFirstName(name);
        person.setLastName("User");
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail(name.toLowerCase() + "-" + suffix + "@test.local");
        person.setPhone("");
        return personRepository.saveAndFlush(person);
    }

    private User saveUser(String login, Person person) {
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash("test-password-hash");
        user.setActive(true);
        user.setPersonId(person.getId());
        return userRepository.saveAndFlush(user);
    }

    private TeachingAssignment teachingAssignmentFor(Person student, String suffix) {
        Faculty faculty = new Faculty();
        faculty.setName("Faculty " + suffix);
        faculty.setCode("F-" + suffix);
        faculty = facultyRepository.saveAndFlush(faculty);

        Group group = new Group();
        group.setName("Group " + suffix);
        group.setCode("G-" + suffix);
        group.setFacultyId(faculty.getId());
        group = groupRepository.saveAndFlush(group);

        GroupMembership studentMembership = new GroupMembership();
        studentMembership.setGroupId(group.getId());
        studentMembership.setPersonId(student.getId());
        studentMembership.setRole(1);
        studentMembership.setStatus(1);
        groupMembershipRepository.saveAndFlush(studentMembership);

        Subject subject = new Subject();
        subject.setName("Subject " + suffix);
        subject = subjectRepository.saveAndFlush(subject);

        Person teacher = person("Teacher", suffix);
        SubjectMembership teacherMembership = new SubjectMembership();
        teacherMembership.setSubjectId(subject.getId());
        teacherMembership.setPersonId(teacher.getId());
        teacherMembership.setRole(1);
        teacherMembership.setStatus(1);
        teacherMembership = subjectMembershipRepository.saveAndFlush(teacherMembership);

        TeachingLoadType loadType = new TeachingLoadType();
        loadType.setName("Lectures " + suffix);
        loadType = teachingLoadTypeRepository.saveAndFlush(loadType);

        TeachingAssignment teachingAssignment = new TeachingAssignment();
        teachingAssignment.setSubjectMembershipId(teacherMembership.getId());
        teachingAssignment.setGroupId(group.getId());
        teachingAssignment.setLoadTypeId(loadType.getId());
        teachingAssignment.setSemester(1);
        teachingAssignment.setAcademicYear(2026);
        teachingAssignment.setHoursPerWeek(BigDecimal.ONE);
        teachingAssignment.setStatus(1);
        return teachingAssignmentRepository.saveAndFlush(teachingAssignment);
    }

    private org.santayn.testing.models.test.Test test(int attemptsAllowed, String suffix) {
        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle("Test " + suffix);
        test.setAttemptsAllowed(attemptsAllowed);
        test.setQuestionCount(1);
        return testRepository.saveAndFlush(test);
    }

    private TestAssignment assignment(org.santayn.testing.models.test.Test test, Integer teachingAssignmentId) {
        TestAssignment assignment = new TestAssignment();
        assignment.setTestId(test.getId());
        assignment.setScope(teachingAssignmentId == null ? 1 : 4);
        assignment.setTeachingAssignmentId(teachingAssignmentId);
        assignment.setAvailableFromUtc(Instant.now().minusSeconds(60));
        assignment.setAvailableUntilUtc(Instant.now().plusSeconds(3600));
        assignment.setStatus(2);
        return testAssignmentRepository.saveAndFlush(assignment);
    }

    private static String uniqueSuffix() {
        return Long.toUnsignedString(System.nanoTime());
    }
}
