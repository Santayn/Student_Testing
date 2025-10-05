// src/main/java/org/santayn/testing/web/controller/rest/QuestionUploadRestController.java
package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.TopicRepository;
import org.santayn.testing.service.QuestionService;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.UserSearch;
import org.santayn.testing.web.dto.question.QuestionListItemDto;
import org.santayn.testing.web.dto.topic.TopicShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

    /** Получить темы по предмету (для выпадающего списка). */
    @GetMapping("/topics")
    public List<TopicShortDto> topicsBySubject(@RequestParam Integer subjectId) {
        assertSubjectOwnedByCurrentTeacher(subjectId);
        return topicRepository.findBySubjectId(subjectId)
                .stream()
                .map(TopicShortDto::from)
                .toList();
    }

    /** Список вопросов по теме — через path variable. */
    @GetMapping("/by-topic/{topicId}")
    public List<QuestionListItemDto> listByTopicPath(@PathVariable Integer topicId) {
        return listByTopicInternal(topicId);
    }

    /** Список вопросов по теме — через query param (?topicId=). */
    @GetMapping("/by-topic")
    public List<QuestionListItemDto> listByTopicQuery(@RequestParam Integer topicId) {
        return listByTopicInternal(topicId);
    }

    /** Добавить один вопрос вручную. */
    @PostMapping("/manual")
    public QuestionListItemDto addManual(@RequestParam Integer topicId,
                                         @RequestParam String text,
                                         @RequestParam String correctAnswer) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тема не найдена"));
        assertSubjectOwnedByCurrentTeacher(topic.getSubject().getId());

        Question q = questionService.createManual(topicId, text, correctAnswer);
        return QuestionListItemDto.from(q);
    }

    /** Загрузка файла с вопросами (.docx или .csv). */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResp upload(@RequestParam Integer topicId,
                             @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл не выбран или пустой");
        }
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тема не найдена"));
        assertSubjectOwnedByCurrentTeacher(topic.getSubject().getId());

        int saved = questionService.processQuestionFileCount(topicId, file);
        return new UploadResp(saved, "OK");
    }

    /**
     * Пакетное удаление (вариант для текущего фронта):
     * DELETE /api/v1/questions/by-topic/{topicId}?ids=1&ids=2&ids=3
     */
    @DeleteMapping("/by-topic/{topicId}")
    public DeleteResult deleteByTopicIds(@PathVariable Integer topicId,
                                         @RequestParam("ids") List<Integer> ids) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тема не найдена"));
        assertSubjectOwnedByCurrentTeacher(topic.getSubject().getId());

        int deleted = questionService.deleteQuestionsByIds(topicId, ids);
        return new DeleteResult(deleted);
    }

    /** Альтернативный вариант пакетного удаления через POST c JSON-массивом в теле. */
    @PostMapping("/batch-delete")
    public DeleteResult batchDelete(@RequestParam Integer topicId,
                                    @RequestBody List<Integer> questionIds) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тема не найдена"));
        assertSubjectOwnedByCurrentTeacher(topic.getSubject().getId());

        int deleted = questionService.deleteQuestionsByIds(topicId, questionIds);
        return new DeleteResult(deleted);
    }

    /* ================= helpers ================= */

    private List<QuestionListItemDto> listByTopicInternal(Integer topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тема не найдена"));
        assertSubjectOwnedByCurrentTeacher(topic.getSubject().getId());

        return questionService.getQuestionsByTopic(topicId)
                .stream()
                .map(QuestionListItemDto::from)
                .toList();
    }

    private void assertSubjectOwnedByCurrentTeacher(Integer subjectId) {
        Teacher t = userSearch.getCurrentTeacher();
        boolean ok = subjectService.getSubjectsByTeacher(t).stream()
                .anyMatch(s -> s.getId().equals(subjectId));
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к предмету id=" + subjectId);
        }
    }

    /* ================= DTOs ответа ================= */

    public record UploadResp(int saved, String status) {}
    public record DeleteResult(int deleted) {}
}
