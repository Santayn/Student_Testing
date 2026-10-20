package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.CurrentUserAccessService;
import org.santayn.testing.service.ResponseReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** Проверка ответов преподавателем: очередь «На проверку» и ручная оценка. */
@RestController
@RequestMapping("/api/v1/results/teacher")
public class ReviewRestController {

    private final ResponseReviewService reviewService;
    private final CurrentUserAccessService accessService;

    public ReviewRestController(ResponseReviewService reviewService, CurrentUserAccessService accessService) {
        this.reviewService = reviewService;
        this.accessService = accessService;
    }

    @GetMapping("/review")
    public ResponseReviewService.ReviewQueue queue(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Integer testId,
                                                   Authentication authentication) {
        return reviewService.queue(teacherScope(authentication), status, testId);
    }

    @PutMapping("/responses/{responseId}/grade")
    public ResponseReviewService.ReviewItem grade(@PathVariable Long responseId,
                                                  @Valid @RequestBody GradeRequest request,
                                                  Authentication authentication) {
        return reviewService.grade(
                teacherScope(authentication),
                accessService.currentUser(authentication).personId(),
                responseId,
                request.awardedPoints(),
                request.comment()
        );
    }

    @PostMapping("/responses/{responseId}/accept")
    public ResponseReviewService.ReviewItem accept(@PathVariable Long responseId, Authentication authentication) {
        return reviewService.accept(
                teacherScope(authentication),
                accessService.currentUser(authentication).personId(),
                responseId
        );
    }

    @PostMapping("/responses/{responseId}/ai-regrade")
    public ResponseReviewService.ReviewItem regrade(@PathVariable Long responseId, Authentication authentication) {
        return reviewService.regrade(teacherScope(authentication), responseId);
    }

    /** null — администратор: видит ответы по всем тестам. */
    private Integer teacherScope(Authentication authentication) {
        return accessService.isAdmin(authentication) ? null : accessService.currentPersonId(authentication);
    }

    public record GradeRequest(@NotNull BigDecimal awardedPoints, @Size(max = 2000) String comment) {
    }
}
