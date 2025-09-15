// src/main/java/org/santayn/testing/web/controller/rest/QuestionUploadRestController.java
package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.service.QuestionService;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.UserSearch;
import org.santayn.testing.web.dto.question.QuestionUploadResponse;
import org.santayn.testing.web.dto.topic.TopicShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.santayn.testing.repository.TopicRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionUploadRestController {

    private final QuestionService questionService;
    private final TopicRepository topicRepository;
    private final SubjectService subjectService;
    private final UserSearch userSearch;

    public QuestionUploadRestController(QuestionService questionService,
                                        TopicRepository topicRepository,
                                        SubjectService subjectService,
                                        UserSearch userSearch) {
        this.questionService = questionService;
        this.topicRepository = topicRepository;
        this.subjectService = subjectService;
        this.userSearch = userSearch;
    }

    /** Темы по предмету (проверяем, что предмет принадлежит текущему преподавателю) */
    @GetMapping("/topics")
    public List<TopicShortDto> topicsBySubject(@RequestParam Integer subjectId) {
        assertSubjectOwnedByCurrentTeacher(subjectId);
        return topicRepository.findBySubjectId(subjectId).stream()
                .map(TopicShortDto::from)
                .toList();
    }

    /** Загрузка файла с вопросами (multipart: topicId + file) */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuestionUploadResponse upload(@RequestParam Integer topicId,
                                         @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл не выбран или пустой");
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тема не найдена"));

        Integer subjectId = topic.getSubject() != null ? topic.getSubject().getId() : null;
        if (subjectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У темы отсутствует предмет");
        }
        assertSubjectOwnedByCurrentTeacher(subjectId);

        int saved = questionService.processQuestionFileCount(topicId, file);
        return new QuestionUploadResponse(saved, "OK");
    }

    private void assertSubjectOwnedByCurrentTeacher(Integer subjectId) {
        Teacher t = userSearch.getCurrentTeacher();
        boolean ok = subjectService.getSubjectsByTeacher(t).stream()
                .anyMatch(s -> s.getId().equals(subjectId));
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к предмету id=" + subjectId);
    }
}
