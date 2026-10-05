package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.service.CurrentUserAccessService;
import org.santayn.testing.service.QuestionService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionUploadRestController {

    private final QuestionService questionService;
    private final CurrentUserAccessService accessService;

    public QuestionUploadRestController(QuestionService questionService, CurrentUserAccessService accessService) {
        this.questionService = questionService;
        this.accessService = accessService;
    }

    @GetMapping
    public List<ApiResponses.QuestionResponseDto> questions(@RequestParam(required = false) Integer testId,
                                                            @RequestParam(required = false) Integer topicId,
                                                            Authentication authentication) {
        if (!accessService.isAdmin(authentication)) {
            if (topicId != null) {
                accessService.requireTopicOwner(authentication, topicId);
            } else if (testId != null) {
                accessService.requireTestOwner(authentication, testId);
            } else {
                throw new AccessDeniedException("Teacher question queries require an owned topicId or testId.");
            }
        }
        return ApiResponses.list(questionService.findAll(testId, topicId), ApiResponses::question);
    }

    @GetMapping("/{questionId}")
    public ApiResponses.QuestionResponseDto one(@PathVariable Long questionId, Authentication authentication) {
        accessService.requireQuestionOwner(authentication, questionId);
        return ApiResponses.question(questionService.get(questionId));
    }

    @GetMapping("/{questionId}/options")
    public List<ApiResponses.QuestionOptionResponse> options(@PathVariable Long questionId, Authentication authentication) {
        accessService.requireQuestionOwner(authentication, questionId);
        return ApiResponses.list(questionService.findOptions(questionId), ApiResponses::questionOption);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuestionImportResponse importDocx(@RequestParam(required = false) Integer testId,
                                             @RequestParam(required = false) Integer courseLectureId,
                                             @RequestParam(required = false) Integer topicId,
                                             @RequestParam("file") MultipartFile file,
                                             Authentication authentication) {
        accessService.requireQuestionContextOwner(authentication, topicId, courseLectureId, testId);
        QuestionService.QuestionImportResult result = questionService.importDocx(testId, courseLectureId, topicId, file);
        return new QuestionImportResponse(
                result.questions().size(),
                result.importedOptions(),
                ApiResponses.list(result.questions(), ApiResponses::question)
        );
    }

    @PostMapping
    public ApiResponses.QuestionResponseDto create(@Valid @RequestBody QuestionRequest request,
                                                   Authentication authentication) {
        accessService.requireQuestionContextOwner(
                authentication, request.topicId(), request.courseLectureId(), request.testId()
        );
        return ApiResponses.question(questionService.create(
                request.testId(),
                request.courseLectureId(),
                request.topicId(),
                request.type(),
                request.question(),
                request.points(),
                request.ordinal(),
                request.correctAnswer(),
                matchingPairs(request.matchingPairs())
        ));
    }

    @PutMapping("/{questionId}")
    public ApiResponses.QuestionResponseDto update(@PathVariable Long questionId,
                                                   @Valid @RequestBody QuestionUpdateRequest request,
                                                   Authentication authentication) {
        accessService.requireQuestionOwner(authentication, questionId);
        if (request.topicId() != null || request.courseLectureId() != null) {
            accessService.requireQuestionContextOwner(
                    authentication, request.topicId(), request.courseLectureId(), null
            );
        } else if (!accessService.isAdmin(authentication)
                && questionService.get(questionId).getTestId() == null) {
            throw new AccessDeniedException("Teacher question cannot be detached from its owned topic or lecture.");
        }
        return ApiResponses.question(questionService.update(
                questionId,
                request.courseLectureId(),
                request.topicId(),
                request.type(),
                request.question(),
                request.points(),
                request.ordinal(),
                request.correctAnswer(),
                matchingPairs(request.matchingPairs()),
                request.active()
        ));
    }

    @GetMapping("/options/{optionId}")
    public ApiResponses.QuestionOptionResponse option(@PathVariable Long optionId, Authentication authentication) {
        accessService.requireOptionOwner(authentication, optionId);
        return ApiResponses.questionOption(questionService.getOption(optionId));
    }

    @PostMapping("/{questionId}/options")
    public ApiResponses.QuestionOptionResponse addOption(@PathVariable Long questionId,
                                                         @Valid @RequestBody OptionRequest request,
                                                         Authentication authentication) {
        accessService.requireQuestionOwner(authentication, questionId);
        return ApiResponses.questionOption(questionService.addOption(questionId, request.text(), request.ordinal(), request.correct()));
    }

    @PutMapping("/options/{optionId}")
    public ApiResponses.QuestionOptionResponse updateOption(@PathVariable Long optionId,
                                                            @Valid @RequestBody OptionRequest request,
                                                            Authentication authentication) {
        accessService.requireOptionOwner(authentication, optionId);
        return ApiResponses.questionOption(questionService.updateOption(optionId, request.text(), request.ordinal(), request.correct()));
    }

    @PutMapping("/{questionId}/active")
    public ApiResponses.QuestionResponseDto setActive(@PathVariable Long questionId,
                                                      @Valid @RequestBody QuestionActiveRequest request,
                                                      Authentication authentication) {
        accessService.requireQuestionOwner(authentication, questionId);
        return ApiResponses.question(questionService.setActive(questionId, request.active()));
    }

    public record QuestionRequest(
            Integer testId,
            Integer courseLectureId,
            Integer topicId,
            int type,
            @NotBlank @Size(max = 2000) String question,
            BigDecimal points,
            @Positive int ordinal,
            @Size(max = 4000) String correctAnswer,
            List<@Valid MatchingPairRequest> matchingPairs
    ) {
    }

    public record QuestionUpdateRequest(
            Integer courseLectureId,
            Integer topicId,
            int type,
            @NotBlank @Size(max = 2000) String question,
            BigDecimal points,
            @Positive int ordinal,
            @Size(max = 4000) String correctAnswer,
            List<@Valid MatchingPairRequest> matchingPairs,
            boolean active
    ) {
    }

    public record OptionRequest(
            @NotBlank @Size(max = 2000) String text,
            @Positive int ordinal,
            boolean correct
    ) {
    }

    public record QuestionActiveRequest(boolean active) {
    }

    public record QuestionImportResponse(int importedQuestions,
                                         int importedOptions,
                                         List<ApiResponses.QuestionResponseDto> questions) {
    }

    public record MatchingPairRequest(
            @NotBlank @Size(max = 2000) String left,
            @NotBlank @Size(max = 2000) String right,
            @Positive int ordinal
    ) {
    }

    private static List<QuestionTypeSupport.MatchingPair> matchingPairs(List<MatchingPairRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(request -> new QuestionTypeSupport.MatchingPair(
                        request.ordinal(),
                        request.left(),
                        request.right()
                ))
                .toList();
    }
}
