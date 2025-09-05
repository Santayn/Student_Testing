package org.santayn.testing.controller;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.answer.AnswerResult;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.repository.AnswerResultRepository;
import org.santayn.testing.repository.TeacherGroupRepository;
import org.santayn.testing.service.StudentService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ResultsController {

    private final AnswerResultRepository answerRepo;
    private final StudentService studentService;
    private final TeacherGroupRepository teacherGroupRepository;

    @GetMapping("/kubstuTest/results")
    public String viewResults(
            Authentication auth,
            Principal principal,
            Model model,
            @RequestParam(value = "groupId", required = false) Integer groupId,
            @RequestParam(value = "studentId", required = false) Integer studentId,
            @RequestParam(value = "testId", required = false) Integer testId,
            @RequestParam(value = "format", required = false) String format
    ) {
        // Проверка аутентификации
        if (auth == null || !auth.isAuthenticated() || principal == null || principal.getName() == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));

        List<AnswerResult> results = Collections.emptyList();
        List<Group> allowedGroups = Collections.emptyList();
        List<Student> studentsOfGroup = Collections.emptyList();

        try {
            if (isTeacher) {
                // Учитель: получаем группы, к которым он прикреплён
                allowedGroups = teacherGroupRepository.findGroupsByTeacherLogin(username);
                model.addAttribute("groups", allowedGroups);

                // Если выбрана группа — проверяем доступ и фильтруем
                if (groupId != null) {
                    Group selectedGroup = allowedGroups.stream()
                            .filter(g -> Objects.equals(g.getId(), groupId))
                            .findFirst()
                            .orElse(null);

                    if (selectedGroup == null) {
                        model.addAttribute("errorMessage", "У вас нет доступа к этой группе.");
                    } else {
                        studentsOfGroup = studentService.getStudentsByGroupId(groupId);
                        model.addAttribute("students", studentsOfGroup);

                        if (studentId != null && testId != null) {
                            results = answerRepo.findByGroupStudentAndTest(groupId, studentId, testId);
                        } else if (studentId != null) {
                            results = answerRepo.findByGroupIdAndStudentId(groupId, studentId);
                        } else {
                            results = answerRepo.findByGroupId(groupId);
                        }
                    }
                }
            } else {
                // Студент: получаем его данные и результаты
                Student student = studentService.findByLogin(username);
                if (student == null) {
                    throw new UsernameNotFoundException("Студент не найден: " + username);
                }
                results = answerRepo.findByStudentId(student.getId());
            }

        } catch (Exception ex) {
            results = Collections.emptyList();
            String errorMsg = isTeacher ? "Ошибка при загрузке результатов для преподавателя." :
                    "Учётная запись студента не найдена: " + username;
            model.addAttribute("errorMessage", errorMsg);
        }

        // Режим JSON
        if ("json".equalsIgnoreCase(format)) {
            model.addAttribute("results", results);
            return "forward:/kubstuTest/results.json";
        }

        // Режим HTML
        model.addAttribute("results", results);
        model.addAttribute("isTeacher", isTeacher);
        model.addAttribute("username", username);
        model.addAttribute("selectedGroupId", groupId);
        model.addAttribute("selectedStudentId", studentId);
        model.addAttribute("selectedTestId", testId);

        return "result-list";
    }

    /**
     * Возвращает результаты в формате JSON.
     */
    @GetMapping(value = "/kubstuTest/results.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> viewResultsJson(
            Authentication auth,
            Principal principal,
            @RequestParam(value = "groupId", required = false) Integer groupId,
            @RequestParam(value = "studentId", required = false) Integer studentId,
            @RequestParam(value = "testId", required = false) Integer testId
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (auth == null || !auth.isAuthenticated() || principal == null || principal.getName() == null) {
            response.put("error", "unauthorized");
            return response;
        }

        String username = principal.getName();
        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));

        response.put("isTeacher", isTeacher);
        response.put("username", username);

        List<AnswerResult> results = new ArrayList<>();

        try {
            if (isTeacher) {
                List<Group> allowedGroups = teacherGroupRepository.findGroupsByTeacherLogin(username);
                response.put("groups", allowedGroups);

                if (groupId != null) {
                    boolean hasAccess = allowedGroups.stream()
                            .anyMatch(g -> Objects.equals(g.getId(), groupId));
                    if (!hasAccess) {
                        response.put("error", "no_access_to_group");
                        return response;
                    }

                    if (studentId != null && testId != null) {
                        results = answerRepo.findByGroupStudentAndTest(groupId, studentId, testId);
                    } else if (studentId != null) {
                        results = answerRepo.findByGroupIdAndStudentId(groupId, studentId);
                    } else {
                        results = answerRepo.findByGroupId(groupId);
                    }

                    response.put("students", studentService.getStudentsByGroupId(groupId));
                }
            } else {
                Student student = studentService.findByLogin(username);
                if (student == null) {
                    response.put("error", "student_not_found");
                    return response;
                }
                results = answerRepo.findByStudentId(student.getId());
            }

        } catch (Exception ex) {
            response.put("error", "internal_error");
            response.put("message", ex.getMessage());
            return response;
        }

        response.put("results", results);
        return response;
    }
}