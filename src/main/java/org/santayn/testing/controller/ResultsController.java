package org.santayn.testing.controller;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.answer.AnswerResult;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.service.ResultsFacadeService;
import org.santayn.testing.service.UserSearch;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ResultsController {

    private final ResultsFacadeService service;
    private final UserSearch userSearch;

    @GetMapping("/kubstuTest/results")
    public String resultsPage(
            Authentication auth,
            Model model,
            @RequestParam(value = "subjectId", required = false) Integer subjectId,
            @RequestParam(value = "lectureId", required = false) Integer lectureId,
            @RequestParam(value = "testId", required = false) Integer testId,
            @RequestParam(value = "groupId", required = false) Integer groupId,
            @RequestParam(value = "studentId", required = false) Integer studentId
    ) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        final String login = auth.getName();
        model.addAttribute("username", login);

        // Проверяем, что это преподаватель, и кладём флаг в модель
        boolean isTeacher;
        try {
            userSearch.getCurrentTeacher();
            isTeacher = true;
        } catch (RuntimeException e) {
            isTeacher = false;
        }
        model.addAttribute("isTeacher", isTeacher);

        if (!isTeacher) {
            // Фильтры скрыты, показываем сообщение и пустые данные — чтобы шаблон не ломался
            model.addAttribute("errorMessage", "Доступ к фильтрам результатов только для преподавателей.");
            model.addAttribute("results", Collections.emptyList());
            model.addAttribute("statsTotal", 0);
            model.addAttribute("statsRight", 0);
            model.addAttribute("statsPercent", 0);

            // на всякий случай — чтобы th:each не падали, если кто-то оставил блоки без th:if
            model.addAttribute("subjects", Collections.emptyList());
            model.addAttribute("lectures", Collections.emptyList());
            model.addAttribute("tests", Collections.emptyList());
            model.addAttribute("groups", Collections.emptyList());
            model.addAttribute("students", Collections.emptyList());
            return "result-list";
        }

        // ===== Преподаватель: загружаем каскад =====

        // 1) Предметы
        List<Subject> subjects = service.allSubjects();
        model.addAttribute("subjects", subjects);

        // 2) Лекции по выбранному предмету
        List<Lecture> lectures = service.lecturesBySubject(subjectId);
        model.addAttribute("lectures", lectures);

        // 3) Тесты по выбранной лекции
        List<Test> tests = service.testsByLecture(lectureId);
        model.addAttribute("tests", tests);

        // 4) Группы, имеющие доступ к выбранному тесту (с учётом преподавателя)
        List<Group> groups = service.groupsByTest(testId, login);
        model.addAttribute("groups", groups);

        // 5) Студенты выбранной группы
        List<Student> students = service.studentsByGroup(groupId);
        model.addAttribute("students", students);

        // Имя выбранного студента (для вывода над фильтрами)
        model.addAttribute("selectedStudentName", service.studentName(studentId));

        // Результаты и статистика
        List<AnswerResult> results = service.loadResults(testId, groupId, studentId);
        int total   = service.countTotal(results);
        int right   = service.countRight(results);
        int percent = service.percent(right, total);

        model.addAttribute("results", results);
        model.addAttribute("statsTotal", total);
        model.addAttribute("statsRight", right);
        model.addAttribute("statsPercent", percent);

        // Сохраняем выбранные значения (чтобы селекты знали, что выбрано)
        model.addAttribute("selectedSubjectId", subjectId);
        model.addAttribute("selectedLectureId", lectureId);
        model.addAttribute("selectedTestId", testId);
        model.addAttribute("selectedGroupId", groupId);
        model.addAttribute("selectedStudentId", studentId);

        return "result-list";
    }
}
