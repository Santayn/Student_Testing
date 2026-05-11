package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.TopicService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping({"/api/topics", "/api/v1/topics"})
public class TopicRestController {

    private final TopicService topicService;

    public TopicRestController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<ApiResponses.TopicResponse> all(@RequestParam(required = false) Integer subjectId,
                                                @RequestParam(required = false) Integer courseLectureId,
                                                @RequestParam(required = false) Integer subjectMembershipId) {
        return ApiResponses.list(topicService.findAll(subjectId, courseLectureId, subjectMembershipId), ApiResponses::topic);
    }

    @GetMapping("/{id}")
    public ApiResponses.TopicResponse one(@PathVariable Integer id) {
        return ApiResponses.topic(topicService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponses.TopicResponse create(@Valid @RequestBody TopicRequest request) {
        return ApiResponses.topic(topicService.create(
                request.subjectId(),
                request.courseLectureId(),
                request.subjectMembershipId(),
                request.ordinal(),
                request.name(),
                request.description()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponses.TopicResponse update(@PathVariable Integer id, @Valid @RequestBody TopicRequest request) {
        return ApiResponses.topic(topicService.update(
                id,
                request.subjectId(),
                request.courseLectureId(),
                request.subjectMembershipId(),
                request.ordinal(),
                request.name(),
                request.description()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        topicService.delete(id);
    }

    public record TopicRequest(
            Integer subjectId,
            Integer courseLectureId,
            Integer subjectMembershipId,
            @Positive int ordinal,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description
    ) {
    }
}
