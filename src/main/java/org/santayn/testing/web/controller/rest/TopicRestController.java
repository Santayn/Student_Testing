package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.TopicService;
import org.santayn.testing.service.UserSearch;
import org.santayn.testing.web.dto.topic.TopicDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/topics")
public class TopicRestController {

    private final TopicService topicService;
    private final SubjectService subjectService;
    private final UserSearch userSearch;

    public TopicRestController(TopicService topicService,
                               SubjectService subjectService,
                               UserSearch userSearch) {
        this.topicService = topicService;
        this.subjectService = subjectService;
        this.userSearch = userSearch;
    }

    /** Список тем предмета */
    @GetMapping
    public List<TopicDto> list(@PathVariable Integer subjectId) {
        ensureSubjectBelongsToCurrentTeacher(subjectId);
        return topicService.getTopicsBySubjectId(subjectId)
                .stream().map(TopicDto::from).toList();
    }

    /** Создать тему */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TopicDto create(@PathVariable Integer subjectId,
                           @RequestBody CreateTopicRequest body) {
        ensureSubjectBelongsToCurrentTeacher(subjectId);
        if (body == null || body.name() == null || body.name().isBlank()) {
            throw new IllegalArgumentException("Поле 'name' обязательно");
        }
        Topic created = topicService.addTopic(subjectId, body.name().trim());
        return TopicDto.from(created);
    }

    /** Удалить тему */
    @DeleteMapping("/{topicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer subjectId,
                       @PathVariable Integer topicId) {
        ensureSubjectBelongsToCurrentTeacher(subjectId);
        topicService.deleteTopic(topicId);
    }

    private void ensureSubjectBelongsToCurrentTeacher(Integer subjectId) {
        var teacher = userSearch.getCurrentTeacher();
        Subject subj = subjectService.findById(subjectId);
        // если у тебя связь Subject.teacher есть — стоит проверить именно её
        var teacherSubjects = subjectService.getSubjectsByTeacher(teacher);
        boolean ok = teacherSubjects.stream().anyMatch(s -> s.getId().equals(subjectId));
        if (!ok) throw new RuntimeException("Учитель не имеет доступа к этому предмету");
    }

    public record CreateTopicRequest(String name) {}
}
