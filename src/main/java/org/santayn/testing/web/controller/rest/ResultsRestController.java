// src/main/java/org/santayn/testing/web/controller/rest/ResultsRestController.java
package org.santayn.testing.web.controller.rest;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.answer.AnswerResult;
import org.santayn.testing.service.ResultsFacadeService;
import org.santayn.testing.web.dto.group.GroupShortDto;
import org.santayn.testing.web.dto.lecture.LectureShortDto;
import org.santayn.testing.web.dto.results.ResultsResponse;
import org.santayn.testing.web.dto.results.StatsDto;
import org.santayn.testing.web.dto.student.StudentShortDto;
import org.santayn.testing.web.dto.subject.SubjectDto;
import org.santayn.testing.web.dto.test.AnswerResultDto;
import org.santayn.testing.web.dto.test.TestDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher/results")
@RequiredArgsConstructor
public class ResultsRestController {

    private final ResultsFacadeService service;

    /* ========== КАСКАДНЫЕ СПИСКИ ========== */

    @GetMapping("/subjects")
    public List<SubjectDto> subjects() {
        return service.allSubjects().stream().map(SubjectDto::from).toList();
    }

    @GetMapping("/lectures")
    public List<LectureShortDto> lectures(@RequestParam(required = false) Integer subjectId) {
        return service.lecturesBySubject(subjectId).stream().map(LectureShortDto::from).toList();
    }

    @GetMapping("/tests")
    public List<TestDto> tests(@RequestParam(required = false) Integer lectureId) {
        return service.testsByLecture(lectureId).stream().map(t ->
                new TestDto(
                        t.getId(),
                        t.getName(),
                        t.getDescription(),
                        t.getQuestionCount(),
                        t.getTopic() == null ? null : t.getTopic().getId(),
                        t.getTopic() == null ? null : t.getTopic().getName(),
                        null, // lectureId (в этом срезе можно не заполнять)
                        null, // lectureTitle
                        List.of() // groupIds (не требуется для списка)
                )
        ).toList();
    }

    @GetMapping("/groups")
    public List<GroupShortDto> groups(Authentication auth,
                                      @RequestParam(required = false) Integer testId) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return service.groupsByTest(testId, auth.getName()).stream().map(GroupShortDto::from).toList();
    }

    @GetMapping("/students")
    public List<StudentShortDto> students(@RequestParam(required = false) Integer groupId) {
        return service.studentsByGroup(groupId).stream().map(StudentShortDto::from).toList();
    }

    /* ========== ДАННЫЕ ДЛЯ ТАБЛИЦЫ + СТАТИСТИКА ========== */

    @GetMapping("/data")
    public ResultsResponse data(Authentication auth,
                                @RequestParam(required = false) Integer subjectId,
                                @RequestParam(required = false) Integer lectureId,
                                @RequestParam(required = false) Integer testId,
                                @RequestParam(required = false) Integer groupId,
                                @RequestParam(required = false) Integer studentId) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<AnswerResult> results = service.loadResults(testId, groupId, studentId);
        int total   = service.countTotal(results);
        int right   = service.countRight(results);
        int percent = service.percent(right, total);

        String testName    = service.testName(testId);
        String groupName   = service.groupName(groupId);
        String studentName = service.studentName(studentId);

        Map<Integer, String> studentNames = service.studentNamesMap(results);

        return new ResultsResponse(
                results.stream().map(ar ->
                        new AnswerResultDto(
                                ar.getQuestionId(),
                                ar.getQuestionText(),
                                ar.getCorrectAnswer(),
                                ar.getUserAnswer(),   // givenAnswer по твоему DTO
                                ar.isCorrect()
                        )
                ).toList(),
                new StatsDto(total, right, percent),
                testName, groupName, studentName,
                studentNames
        );
    }
}
