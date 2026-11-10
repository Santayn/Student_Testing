package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santayn.testing.models.ai.AiEngine;
import org.santayn.testing.models.ai.AiFeature;
import org.santayn.testing.models.ai.AiJob;
import org.santayn.testing.models.ai.AiJobStatus;
import org.santayn.testing.models.ai.AiJobType;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.question.ResponseReview;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.AiJobRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Собственные модели вместо внешнего API: внешний провайдер выключен, local-ai — тестовый сервер. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:student_test_local_ai;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR",
        "app.ai.provider=none",
        "app.ai.grading.max-attempts=1",
        "app.ai.engines.grading=local",
        "app.ai.engines.lecture-scan=local",
        "app.ai.images.search-enabled=false"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalEngineIntegrationTests {

    private static final LoopbackAiServer LOCAL_AI = startServer();

    @DynamicPropertySource
    static void localAi(DynamicPropertyRegistry registry) {
        registry.add("app.ai.local.base-url", () -> LOCAL_AI.baseUrl().replace("/v1", ""));
    }

    @AfterAll
    static void stopServer() {
        LOCAL_AI.close();
    }

    @Autowired private TestService testService;
    @Autowired private AiJobWorker worker;
    @Autowired private AiJobService jobService;
    @Autowired private AiEngineService engineService;
    @Autowired private TextAnswerGradingService gradingService;
    @Autowired private LectureScanService lectureScanService;
    @Autowired private PersonRepository personRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private TestAssignmentRepository assignmentRepository;
    @Autowired private QuestionResponseRepository responseRepository;
    @Autowired private AiJobRepository jobRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void resetServer() {
        LOCAL_AI.reset();
        LOCAL_AI.respond(request -> switch (request.path()) {
            case "/health" -> LoopbackAiServer.Response.ok(
                    "{\"grader\":{\"ready\":true,\"name\":\"grader-test\",\"metrics\":{\"score_within_0_25\":0.9}},"
                            + "\"layout\":{\"ready\":true,\"name\":\"layout-test\"}}");
            case "/v1/grade" -> LoopbackAiServer.Response.ok(
                    "{\"score\":1.0,\"confidence\":0.92,\"matched\":[\"назначение\",\"пример\"],\"missing\":[],"
                            + "\"feedback\":\"Ответ засчитан полностью.\",\"suspicious\":false}");
            case "/v1/scan" -> LoopbackAiServer.Response.ok(
                    "{\"markdown\":\"## Оперативная память\\n\\n- хранит данные\",\"pages\":1,\"engine\":\"layout-test\"}");
            default -> new LoopbackAiServer.Response(404, "{}");
        });
    }

    @Test
    void freeAnswersAreGradedByOwnModelWithoutExternalApi() throws Exception {
        assertThat(gradingService.isAiGradingAvailable()).isTrue();

        Submitted submitted = submitTextAnswer("Хранит данные программ, пока компьютер включён");
        worker.runReadyJobsNow();

        QuestionResponse response = responseRepository.findById(submitted.responseId()).orElseThrow();
        assertThat(response.getReviewStatus()).isEqualTo(ResponseReview.AI_GRADED);
        assertThat(response.getAwardedPoints()).isEqualByComparingTo("2.00");
        assertThat(response.getAiFeedback()).isEqualTo("Ответ засчитан полностью.");

        LoopbackAiServer.Request request = LOCAL_AI.requests().stream()
                .filter(r -> r.path().equals("/v1/grade")).findFirst().orElseThrow();
        JsonNode body = objectMapper.readTree(request.body());
        assertThat(body.path("criteria").asText()).contains("назначение").contains("пример");
        assertThat(body.path("points").asDouble()).isEqualTo(2.0);
        assertThat(body.path("answer").asText()).startsWith("Хранит данные");

        AiJob job = jobRepository.findAll().stream()
                .filter(j -> AiJobType.TEXT_GRADING.equals(j.getType()) && submitted.responseId().equals(j.getTargetId()))
                .findFirst().orElseThrow();
        assertThat(job.getEngine()).isEqualTo(AiEngine.LOCAL);
        assertThat(job.getInputTokens() + job.getOutputTokens()).isZero();
    }

    @Test
    void ownModelDoesNotLowerScoreWithoutTeacher() {
        // Своя модель может не узнать верный пересказ и уверенно снизить балл — такое решает преподаватель.
        LOCAL_AI.respond(request -> request.path().equals("/v1/grade")
                ? LoopbackAiServer.Response.ok("{\"score\":0.0,\"confidence\":0.95,\"matched\":[],\"missing\":[\"назначение\",\"пример\"],"
                        + "\"feedback\":\"Ни один из ключевых пунктов ответа не раскрыт.\",\"suspicious\":false}")
                : LoopbackAiServer.Response.ok("{\"grader\":{\"ready\":true}}"));

        Submitted submitted = submitTextAnswer("Держит то, с чем сейчас работает процессор, и забывает всё при выключении");
        worker.runReadyJobsNow();

        QuestionResponse response = responseRepository.findById(submitted.responseId()).orElseThrow();
        assertThat(response.getReviewStatus()).isEqualTo(ResponseReview.NEEDS_TEACHER);
    }

    @Test
    void unsureOwnModelSendsAnswerToTeacher() {
        LOCAL_AI.respond(request -> request.path().equals("/v1/grade")
                ? LoopbackAiServer.Response.ok("{\"score\":1.0,\"confidence\":0.2,\"matched\":[],\"missing\":[],\"feedback\":null,\"suspicious\":false}")
                : LoopbackAiServer.Response.ok("{\"grader\":{\"ready\":true}}"));

        Submitted submitted = submitTextAnswer("Что-то про память");
        worker.runReadyJobsNow();

        QuestionResponse response = responseRepository.findById(submitted.responseId()).orElseThrow();
        assertThat(response.getReviewStatus()).isEqualTo(ResponseReview.NEEDS_TEACHER);
    }

    @Test
    void lectureScanIsRecognisedAndStructuredByOwnModel() throws Exception {
        assertThat(lectureScanService.isAvailable()).isTrue();
        MockMultipartFile page = new MockMultipartFile("files", "page.png", "image/png", png());

        AiJob job = lectureScanService.enqueue(null, null, List.of(page), false);
        worker.runReadyJobsNow();

        AiJob done = jobService.get(job.getId());
        assertThat(done.getStatus()).isEqualTo(AiJobStatus.DONE);
        assertThat(done.getEngine()).isEqualTo(AiEngine.LOCAL);
        assertThat(done.getOcrPages()).isEqualTo(1);
        assertThat(jobService.readResult(done).path("markdown").asText()).startsWith("## Оперативная память");
        JsonNode body = objectMapper.readTree(LOCAL_AI.requests().stream()
                .filter(r -> r.path().equals("/v1/scan")).findFirst().orElseThrow().body());
        assertThat(body.path("pages")).hasSize(1);
    }

    @Test
    void administratorSeesStatusAndCannotPickUnconnectedModel() {
        List<AiEngineService.FeatureStatus> overview = engineService.overview();
        AiEngineService.FeatureStatus grading = overview.stream()
                .filter(f -> f.feature().equals("grading")).findFirst().orElseThrow();
        assertThat(grading.engine()).isEqualTo(AiEngine.LOCAL);
        assertThat(grading.apiAvailable()).isFalse();
        assertThat(grading.localReady()).isTrue();
        assertThat(grading.localModel()).isEqualTo("grader-test");

        // Ollama не настроена — своя модель критериев недоступна.
        assertThatThrownBy(() -> engineService.setEngine(AiFeature.CRITERIA, "local", null))
                .isInstanceOf(IllegalArgumentException.class);

        engineService.setEngine(AiFeature.GRADING, "api", null);
        assertThat(engineService.engine(AiFeature.GRADING)).isEqualTo(AiEngine.API);
        assertThat(gradingService.isAiGradingAvailable()).isFalse();
        engineService.setEngine(AiFeature.GRADING, "local", null);
        assertThat(gradingService.isAiGradingAvailable()).isTrue();
    }

    private Submitted submitTextAnswer(String answer) {
        Person person = new Person();
        person.setFirstName("Local");
        person.setLastName("Student");
        person.setDateOfBirth(LocalDate.of(2001, 1, 1));
        person.setEmail("local-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        person = personRepository.saveAndFlush(person);

        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle("Своя проверка");
        test.setAttemptsAllowed(1);
        test.setQuestionCount(1);
        test = testRepository.saveAndFlush(test);

        Question question = new Question();
        question.setTestId(test.getId());
        question.setType(QuestionTypeSupport.TYPE_TEXT);
        question.setQuestion("Что такое ОЗУ?");
        question.setCorrectAnswer("оперативная память");
        question.setGradingCriteria("— назначение — 1 б.\n— пример — 1 б.");
        question.setPoints(new BigDecimal("2.00"));
        question = questionRepository.saveAndFlush(question);

        TestAssignment assignment = new TestAssignment();
        assignment.setTestId(test.getId());
        assignment.setScope(1);
        assignment.setAvailableFromUtc(Instant.now().minusSeconds(60));
        assignment.setAvailableUntilUtc(Instant.now().plusSeconds(3600));
        assignment.setStatus(2);
        assignment = assignmentRepository.saveAndFlush(assignment);

        TestService.TestAttemptQuestionSet set = testService.startOrResumeAttemptWithRandomQuestions(
                assignment.getId(), person.getId(), null
        );
        TestAttempt attempt = set.attempt();
        QuestionResponse response = testService.submitResponse(attempt.getId(), question.getId(), answer, List.of());
        testService.completeAttempt(attempt.getId());
        return new Submitted(response.getId());
    }

    private static byte[] png() {
        try {
            BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private static LoopbackAiServer startServer() {
        try {
            return LoopbackAiServer.start();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private record Submitted(Long responseId) {
    }
}
