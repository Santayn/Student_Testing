package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.TopicRepository;
import org.santayn.testing.service.TestService;
import org.santayn.testing.web.dto.test.TestCreateRequest;
import org.santayn.testing.web.dto.test.TestDto;

@RestController
@RequestMapping(value = "/api/v1/tests", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TestRestController {

    private final TestService testService;
    private final TopicRepository topicRepository;
    private final LectureRepository lectureRepository;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TestDto create(@Valid @RequestBody TestCreateRequest req) {

        Topic topic = topicRepository.findById(req.topicId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Тема не найдена: " + req.topicId()));

        Test test = testService.createAndPopulateTest(
                topic,
                req.lectureId(),
                req.groupIds(),
                req.name(),
                req.description(),
                req.questionCount()
        );

        // подтянем title лекции для ответа (не обязательно, но удобно для фронта)
        String lectureTitle = lectureRepository.findById(req.lectureId())
                .map(Lecture::getTitle)
                .orElse(null);

        return new TestDto(
                test.getId(),
                req.name(),
                req.description(),
                req.questionCount(),
                req.topicId(),
                topic.getName(),
                req.lectureId(),
                lectureTitle,
                req.groupIds()
        );
    }

    // Простой маппер для 400 вместо 500 на валидации сервиса
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> onBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
