package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.ai.AiJob;
import org.santayn.testing.models.ai.AiJobStatus;
import org.santayn.testing.models.ai.AiJobType;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureMaterial;
import org.santayn.testing.models.lecture.LectureTestLink;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.AiJobRepository;
import org.santayn.testing.repository.LectureMaterialRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.LectureTestLinkRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.TopicRepository;
import org.santayn.testing.service.ai.AiJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:student_test_lecture_delete;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LectureDeletionIntegrationTests {

    @Autowired private LectureService lectureService;
    @Autowired private LectureMaterialService lectureMaterialService;
    @Autowired private LectureTestLinkService lectureTestLinkService;
    @Autowired private AiJobService aiJobService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private LectureRepository lectureRepository;
    @Autowired private LectureMaterialRepository lectureMaterialRepository;
    @Autowired private LectureTestLinkRepository lectureTestLinkRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TestAssignmentRepository testAssignmentRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private AiJobRepository aiJobRepository;

    @Value("${app.storage.lecture-materials-dir}")
    private String materialsDir;

    @Test
    void deletesLectureWithMaterialsFilesTopicsTestLinksAndDetachesAiJobs() {
        Lecture lecture = lecture();
        LectureMaterial material = generatedMaterial(lecture.getId());
        Path file = materialFile(material);
        assertThat(file).exists();
        Topic topic = topic(lecture);
        org.santayn.testing.models.test.Test test = test();
        linkTest(lecture.getId(), test.getId());
        TestAssignment lectureAssignment = testAssignmentRepository
                .findFirstByTestIdAndCourseLectureId(test.getId(), lecture.getId())
                .orElseThrow();
        AiJob queued = aiJobService.enqueue(AiJobType.PRESENTATION_OUTLINE, null, lecture.getId().longValue(), Map.of());
        AiJob unrelated = aiJobService.enqueue(AiJobType.TEXT_GRADING, null, lecture.getId().longValue(), Map.of());

        lectureService.delete(lecture.getId());

        assertThat(lectureRepository.existsById(lecture.getId())).isFalse();
        assertThat(lectureMaterialRepository.existsById(material.getId())).isFalse();
        assertThat(file).doesNotExist();
        assertThat(topicRepository.existsById(topic.getId())).isFalse();
        assertThat(lectureTestLinkRepository.findByCourseLectureIdOrderByIdAsc(lecture.getId())).isEmpty();
        assertThat(testAssignmentRepository.existsById(lectureAssignment.getId())).isFalse();
        assertThat(testRepository.existsById(test.getId())).isTrue();

        AiJob failed = aiJobRepository.findById(queued.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(failed.getTargetId()).isNull();
        assertThat(failed.getError()).isEqualTo("Лекция удалена.");
        // targetId задачи проверки ответа — id ответа студента, а не лекции.
        AiJob untouched = aiJobRepository.findById(unrelated.getId()).orElseThrow();
        assertThat(untouched.getStatus()).isEqualTo(AiJobStatus.QUEUED);
        assertThat(untouched.getTargetId()).isEqualTo(lecture.getId().longValue());
    }

    @Test
    void blocksDeletionWhenTopicHasQuestionsAndKeepsMaterials() {
        Lecture lecture = lecture();
        LectureMaterial material = generatedMaterial(lecture.getId());
        Topic topic = topic(lecture);
        Question question = new Question();
        question.setTopicId(topic.getId());
        question.setCourseLectureId(lecture.getId());
        question.setType(QuestionTypeSupport.TYPE_TEXT);
        question.setQuestion("Вопрос");
        question.setCorrectAnswer("ответ");
        questionRepository.saveAndFlush(question);

        assertThatThrownBy(() -> lectureService.delete(lecture.getId()))
                .isInstanceOf(AuthConflictException.class)
                .hasMessageStartingWith("Нельзя удалить лекцию:")
                .hasMessageContaining("вопросы (1)");

        assertThat(lectureRepository.existsById(lecture.getId())).isTrue();
        assertThat(lectureMaterialRepository.existsById(material.getId())).isTrue();
        assertThat(materialFile(material)).exists();
        assertThat(topicRepository.existsById(topic.getId())).isTrue();
    }

    @Test
    void blocksDeletionWhenStudentsAttemptedLectureTest() {
        Lecture lecture = lecture();
        org.santayn.testing.models.test.Test test = test();
        linkTest(lecture.getId(), test.getId());
        TestAssignment assignment = testAssignmentRepository
                .findFirstByTestIdAndCourseLectureId(test.getId(), lecture.getId())
                .orElseThrow();
        TestAttempt attempt = new TestAttempt();
        attempt.setTestAssignmentId(assignment.getId());
        attempt.setPersonId(person().getId());
        testAttemptRepository.saveAndFlush(attempt);

        assertThatThrownBy(() -> lectureService.delete(lecture.getId()))
                .isInstanceOf(AuthConflictException.class)
                .hasMessageContaining("попыток: 1");

        assertThat(lectureRepository.existsById(lecture.getId())).isTrue();
        assertThat(testAssignmentRepository.existsById(assignment.getId())).isTrue();
    }

    @Test
    void keepsMaterialFilesWhenTransactionRollsBack() {
        Lecture lecture = lecture();
        LectureMaterial material = generatedMaterial(lecture.getId());

        transactionTemplate.executeWithoutResult(status -> {
            lectureMaterialService.deleteAllForLecture(lecture.getId());
            status.setRollbackOnly();
        });

        assertThat(lectureMaterialRepository.existsById(material.getId())).isTrue();
        assertThat(materialFile(material)).exists();
    }

    @Test
    void deletesLectureWhoseMaterialFileIsAlreadyMissing() throws Exception {
        Lecture lecture = lecture();
        LectureMaterial material = generatedMaterial(lecture.getId());
        Files.delete(materialFile(material));

        lectureService.delete(lecture.getId());

        assertThat(lectureRepository.existsById(lecture.getId())).isFalse();
        assertThat(lectureMaterialRepository.existsById(material.getId())).isFalse();
    }

    private Lecture lecture() {
        Subject subject = new Subject();
        subject.setName("Предмет " + System.nanoTime());
        subject = subjectRepository.saveAndFlush(subject);
        Lecture lecture = new Lecture();
        lecture.setSubjectId(subject.getId());
        lecture.setTitle("Лекция");
        lecture.setContentFolderKey("delete-test-" + System.nanoTime());
        return lectureRepository.saveAndFlush(lecture);
    }

    private LectureMaterial generatedMaterial(Integer lectureId) {
        return lectureMaterialService.saveGenerated(
                lectureId, "slides.pptx", "application/octet-stream", "content".getBytes(StandardCharsets.UTF_8)
        );
    }

    private Path materialFile(LectureMaterial material) {
        return Path.of(materialsDir).toAbsolutePath().normalize().resolve(material.getStoredPath()).normalize();
    }

    private Topic topic(Lecture lecture) {
        Topic topic = new Topic();
        topic.setSubjectId(lecture.getSubjectId());
        topic.setCourseLectureId(lecture.getId());
        topic.setName("Тема");
        return topicRepository.saveAndFlush(topic);
    }

    private org.santayn.testing.models.test.Test test() {
        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle("Тест лекции");
        test.setAttemptsAllowed(1);
        test.setQuestionCount(1);
        return testRepository.saveAndFlush(test);
    }

    /** Как при привязке теста в интерфейсе: связь и автоматическое назначение со scope «лекция». */
    private void linkTest(Integer lectureId, Integer testId) {
        LectureTestLink link = new LectureTestLink();
        link.setCourseLectureId(lectureId);
        link.setTestId(testId);
        lectureTestLinkRepository.saveAndFlush(link);
        lectureTestLinkService.ensureLectureTestAvailability(lectureId, testId);
        assertThat(lectureTestLinkService.findTestIdsByLectureId(lectureId)).containsExactly(testId);
    }

    private Person person() {
        Person person = new Person();
        person.setFirstName("Студент");
        person.setLastName("Тестовый");
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail("lecture-delete-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        return personRepository.saveAndFlush(person);
    }
}
