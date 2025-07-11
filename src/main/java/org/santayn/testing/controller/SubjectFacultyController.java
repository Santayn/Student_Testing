package org.santayn.testing.controller;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.service.FacultyService;
import org.santayn.testing.service.SubjectFacultyService;
import org.santayn.testing.service.SubjectService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class SubjectFacultyController {

    private final FacultyService facultyService;
    private final SubjectService subjectService;
    private final SubjectFacultyService subjectFacultyService;

    public SubjectFacultyController(FacultyService facultyService, SubjectService subjectService, SubjectFacultyService subjectFacultyService) {
        this.facultyService = facultyService;
        this.subjectService = subjectService;
        this.subjectFacultyService = subjectFacultyService;
    }

    /**
     * Отображает страницу с выбором группы и действий над студентами
     */
    @GetMapping("manage-subject-faculty")
    public String showManageSubjectFacultyPage(
            @RequestParam(required = false) Integer facultyId,
            Model model) {
        // Получаем все группы
        List<Faculty> facultys = facultyService.getAllFaculty();
        System.out.println("Number of faculties: " + facultys.size());
        model.addAttribute("facultys", facultys);

        if (facultyId != null) {
            // Если выбрана группа, получаем студентов в группе и доступных студентов
            List<Subject> subjectsFaculty = subjectService.getSubjectsByFacultyId(facultyId);
            List<Subject> freeSubjects = subjectService.findFreeSubjects();

            model.addAttribute("facultyId", facultyId);
            model.addAttribute("subjectFaculty", subjectsFaculty);
            model.addAttribute("freeSubjects", freeSubjects);
        }

        return "manage-subject-faculty"; // Имя Thymeleaf шаблона
    }
    /**
     * Обрабатывает POST-запрос: добавляет выбранных студентов в выбранную группу
     */
    @PostMapping("add-subject-to-faculty")
    public String addSubjectsToFaculty(
            @RequestParam Integer facultyId,
            @RequestParam List<Integer> selectedSubjectIds) {
        subjectFacultyService.addSubjectsToFaculty(facultyId, selectedSubjectIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-subject-faculty"; // Перезагрузка страницы
    }
    /**
     * Обрабатывает POST-запрос: удаляет выбранных студентов из группы
     */
    @PostMapping("remove-subjects-from-faculty")
    public String removeSubjectsFromFaculty(
            @RequestParam Integer facultyId,
            @RequestParam List<Integer> selectedSubjectIds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current user roles: " + Arrays.toString(auth.getAuthorities().toArray()));

        subjectFacultyService.deleteSubjectsFromFaculty(facultyId, selectedSubjectIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-subject-faculty"; // Перезагрузка страницы
    }
    /**
     * AJAX-эндпоинт для получения студентов в указанной группе
     */
    @GetMapping("subjects-in-faculty/{facultyId}")
    @ResponseBody
    public List<Subject> getSubjectsInFaculty(@PathVariable Integer facultyId) {
        return subjectService.getSubjectsByFacultyId(facultyId);
    }
    /**
     * AJAX-эндпоинт для получения свободных студентов (не входящих в группу)
     */
    @GetMapping("free-subjects/{subjectId}")
    @ResponseBody
    public List<Subject> getFreeSubjects() {
        return subjectFacultyService.findFreeSubjects();
    }
}