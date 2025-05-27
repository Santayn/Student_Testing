package org.santayn.testing.controller;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.TeacherService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class SubjectController {

    private final SubjectService subjectService;
    private final TeacherService teacherService;
    private final GroupService groupService;

    public SubjectController(SubjectService subjectService, TeacherService teacherService,GroupService groupService) {
        this.subjectService = subjectService;
        this.teacherService = teacherService;
        this.groupService = groupService;
    }


    @GetMapping("manage-teachers-subject")
    public String showManageTeachersSubjectsPage(
            @RequestParam(required = false) Integer teacherId,
            Model model) {
        // Получаем все предметы
        List<Teacher> teachers = teacherService.getAllTeacher();
        model.addAttribute("teachers", teachers);

        if (teacherId != null) {
            // Если выбрана группа, получаем студентов в группе и доступных студентов
            List<Subject> teacherSubjects = subjectService.getSubjectsByTecherID(teacherId);
            List<Subject> freeSubjects = subjectService.findFreeSubjects();

            model.addAttribute("teacherId", teacherId);
            model.addAttribute("teacherGroups", teacherSubjects);
            model.addAttribute("freeGroups", freeSubjects);
        }

        return "manage-teachers-subject"; // Имя Thymeleaf шаблона
    }

    @PostMapping("add-subjects-to-teacher")
    public String addSubjectsToTeacher(
            @RequestParam Integer teacherId,
            @RequestParam List<Integer> selectedSubjectIds) {
        teacherService.addSubjectsToTeacher(teacherId, selectedSubjectIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-teachers-subject"; // Перезагрузка страницы
    }
    @PostMapping("remove-subjects-from-teacher")
    public String removeSubjectsFromTeacher(
            @RequestParam Integer teacherId,
            @RequestParam List<Integer> selectedSubjectIds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current user roles: " + Arrays.toString(auth.getAuthorities().toArray()));

        teacherService.deleteSubjectsFromTeacher(teacherId, selectedSubjectIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-teachers-subject"; // Перезагрузка страницы
    }
    @GetMapping("subjects-in-teacher/{teacherId}")
    @ResponseBody
    public List<Subject> getSubjectsInTeacher(@PathVariable Integer teacherId) {
        return subjectService.getSubjectsByTecherID(teacherId);
    }
    @GetMapping("free-subjects/{teacherId}")
    @ResponseBody
    public List<Subject> getFreeSubjects() {
        return subjectService.findFreeSubjects();
    }









    // Отображение списка предметов по ID факультета
    @GetMapping("/subjects/{facultyId}")
    public String getSubjectsByFacultyId(@PathVariable Integer facultyId, Model model) {
        List<Subject> subjects = subjectService.getSubjectsByFacultyId(facultyId);

        if (subjects.isEmpty()) {
            model.addAttribute("error", "Предметы для данного факультета не найдены.");
        } else {
            model.addAttribute("subjects", subjects);
            model.addAttribute("facultyId", facultyId); // Передаем facultyId для ссылок
        }

        return "subjects"; // Имя шаблона Thymeleaf
    }

    // Отображение деталей конкретного предмета
    @GetMapping("/subjects/{facultyId}/{subjectId}")
    public String getSubjectDetails(@PathVariable Integer facultyId,
                                    @PathVariable Integer subjectId,
                                    Model model) {
        try {
            Subject selectedSubject = subjectService.getSpecificSubjectByFacultyIdAndSubjectId(facultyId, subjectId);
            model.addAttribute("selectedSubject", selectedSubject);
            model.addAttribute("facultyId", facultyId); // Передаем facultyId для ссылки "Назад"
            return "subject-details"; // Имя шаблона Thymeleaf для отображения деталей предмета
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/kubstuTest/subjects/" + facultyId; // Возвращаемся к списку предметов с сообщением об ошибке
        }
    }
}