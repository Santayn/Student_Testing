package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.TestService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping({"/api/tests", "/api/v1/tests"})
public class TestRestController {

    private final TestService testService;

    public TestRestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping
    public List<ApiResponses.TestResponse> all(@RequestParam(required = false) Integer subjectId) {
        return ApiResponses.list(testService.findAll(subjectId), ApiResponses::test);
    }

    @GetMapping("/{id}")
    public ApiResponses.TestResponse one(@PathVariable Integer id) {
        return ApiResponses.test(testService.get(id));
    }

    @PostMapping
    public ApiResponses.TestResponse create(@Valid @RequestBody TestRequest request) {
        return ApiResponses.test(testService.create(
                request.title(),
                request.description(),
                request.duration(),
                request.attemptsAllowed(),
                request.questionCount(),
                selectionRuleInputs(request.selectionRules())
        ));
    }

    @PutMapping("/{id}")
    public ApiResponses.TestResponse update(@PathVariable Integer id, @Valid @RequestBody TestRequest request) {
        return ApiResponses.test(testService.update(
                id,
                request.title(),
                request.description(),
                request.duration(),
                request.attemptsAllowed(),
                request.questionCount(),
                request.selectionRules() == null ? null : selectionRuleInputs(request.selectionRules())
        ));
    }

    @GetMapping("/{testId}/selection-rules")
    public List<ApiResponses.TestQuestionSelectionRuleResponse> selectionRules(@PathVariable Integer testId) {
        return ApiResponses.list(testService.findSelectionRules(testId), ApiResponses::testQuestionSelectionRule);
    }

    @PutMapping("/{testId}/selection-rules")
    public List<ApiResponses.TestQuestionSelectionRuleResponse> replaceSelectionRules(
            @PathVariable Integer testId,
            @Valid @RequestBody List<@Valid SelectionRuleRequest> request) {
        return ApiResponses.list(
                testService.replaceSelectionRules(testId, selectionRuleInputs(request)),
                ApiResponses::testQuestionSelectionRule
        );
    }

    @GetMapping("/assignments")
    public List<ApiResponses.TestAssignmentResponse> assignments(@RequestParam(required = false) Integer testId,
                                                                 @RequestParam(required = false) Integer courseVersionId,
                                                                 @RequestParam(required = false) Integer courseLectureId,
                                                                 @RequestParam(required = false) Integer teachingAssignmentId) {
        return ApiResponses.list(
                testService.findAssignments(testId, courseVersionId, courseLectureId, teachingAssignmentId),
                ApiResponses::testAssignment
        );
    }

    @GetMapping("/assignments/{assignmentId}")
    public ApiResponses.TestAssignmentResponse assignment(@PathVariable Integer assignmentId) {
        return ApiResponses.testAssignment(testService.getAssignment(assignmentId));
    }

    @PostMapping("/{testId}/assignments")
    public ApiResponses.TestAssignmentResponse assign(@PathVariable Integer testId, @Valid @RequestBody AssignmentRequest request) {
        return ApiResponses.testAssignment(testService.assign(
                testId,
                request.scope(),
                request.courseVersionId(),
                request.courseLectureId(),
                request.teachingAssignmentId(),
                request.availableFromUtc(),
                request.availableUntilUtc(),
                request.status()
        ));
    }

    @PutMapping("/assignments/{assignmentId}")
    public ApiResponses.TestAssignmentResponse updateAssignment(@PathVariable Integer assignmentId,
                                                                @Valid @RequestBody AssignmentRequest request) {
        return ApiResponses.testAssignment(testService.updateAssignment(
                assignmentId,
                request.scope(),
                request.courseVersionId(),
                request.courseLectureId(),
                request.teachingAssignmentId(),
                request.availableFromUtc(),
                request.availableUntilUtc(),
                request.status()
        ));
    }

    @PutMapping("/assignments/{assignmentId}/status")
    public ApiResponses.TestAssignmentResponse updateAssignmentStatus(@PathVariable Integer assignmentId,
                                                                      @Valid @RequestBody AssignmentStatusRequest request) {
        return ApiResponses.testAssignment(testService.updateAssignmentStatus(assignmentId, request.status()));
    }

    @GetMapping("/attempts")
    public List<ApiResponses.TestAttemptResponse> attempts(@RequestParam(required = false) Integer testAssignmentId,
                                                           @RequestParam(required = false) Integer personId) {
        return ApiResponses.list(testService.findAttempts(testAssignmentId, personId), ApiResponses::testAttempt);
    }

    @GetMapping("/attempts/{attemptId}")
    public ApiResponses.TestAttemptResponse attempt(@PathVariable Integer attemptId) {
        return ApiResponses.testAttempt(testService.getAttempt(attemptId));
    }

    @PostMapping("/assignments/{assignmentId}/attempts")
    public ApiResponses.TestAttemptResponse startAttempt(@PathVariable Integer assignmentId,
                                                         @Valid @RequestBody StartAttemptRequest request) {
        return ApiResponses.testAttempt(testService.startAttempt(
                assignmentId,
                request.personId(),
                request.teachingAssignmentEnrollmentId()
        ));
    }

    @GetMapping("/attempts/{attemptId}/responses")
    public List<ApiResponses.QuestionAnswerResponse> responses(@PathVariable Integer attemptId) {
        return ApiResponses.list(testService.findResponses(attemptId), ApiResponses::questionResponse);
    }

    @PostMapping("/attempts/{attemptId}/responses")
    public ApiResponses.QuestionAnswerResponse submitResponse(@PathVariable Integer attemptId,
                                                              @Valid @RequestBody ResponseRequest request) {
        return ApiResponses.questionResponse(testService.submitResponse(
                attemptId,
                request.testQuestionId(),
                request.answerText(),
                request.selectedOptionIds(),
                request.awardedPoints(),
                request.correct()
        ));
    }

    @GetMapping("/responses/{responseId}/selected-options")
    public List<ApiResponses.SelectedOptionResponse> selectedOptions(@PathVariable Long responseId) {
        return ApiResponses.list(testService.findSelectedOptions(responseId), ApiResponses::selectedOption);
    }

    @PostMapping("/attempts/{attemptId}/complete")
    public ApiResponses.TestAttemptResponse completeAttempt(@PathVariable Integer attemptId,
                                                            @Valid @RequestBody CompleteAttemptRequest request) {
        return ApiResponses.testAttempt(testService.completeAttempt(attemptId, request.score(), request.status()));
    }

    public record TestRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime duration,
            @Positive int attemptsAllowed,
            @Positive int questionCount,
            List<@Valid SelectionRuleRequest> selectionRules
    ) {
    }

    public record SelectionRuleRequest(
            Integer courseLectureId,
            Integer topicId,
            @Positive int questionCount,
            @Min(0) int textQuestionCount,
            @Min(0) int singleAnswerQuestionCount,
            @Min(0) int multipleAnswerQuestionCount,
            @Min(0) int matchingQuestionCount,
            @Min(0) int ordinal
    ) {
    }

    public record AssignmentRequest(
            @Min(1) @Max(4) int scope,
            Integer courseVersionId,
            Integer courseLectureId,
            Integer teachingAssignmentId,
            @NotNull
            Instant availableFromUtc,
            @NotNull
            Instant availableUntilUtc,
            @Min(1) @Max(4) int status
    ) {
    }

    public record AssignmentStatusRequest(
            @Min(1) @Max(4) int status
    ) {
    }

    public record StartAttemptRequest(
            @NotNull Integer personId,
            Integer teachingAssignmentEnrollmentId
    ) {
    }

    public record ResponseRequest(
            @NotNull Long testQuestionId,
            @Size(max = 4000) String answerText,
            List<Long> selectedOptionIds,
            @DecimalMin("0.00") BigDecimal awardedPoints,
            Boolean correct
    ) {
    }

    public record CompleteAttemptRequest(
            @DecimalMin("0.00") BigDecimal score,
            @Min(2) @Max(3) Integer status
    ) {
    }

    private static List<TestService.SelectionRuleInput> selectionRuleInputs(List<SelectionRuleRequest> rules) {
        if (rules == null) {
            return List.of();
        }
        return rules.stream()
                .map(rule -> new TestService.SelectionRuleInput(
                        rule.courseLectureId(),
                        rule.topicId(),
                        rule.questionCount(),
                        rule.textQuestionCount(),
                        rule.singleAnswerQuestionCount(),
                        rule.multipleAnswerQuestionCount(),
                        rule.matchingQuestionCount(),
                        rule.ordinal()
                ))
                .toList();
    }
}
