package org.santayn.testing.controller;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.teacher.Teacher_Subject;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.repository.*;
import org.santayn.testing.service.QuestionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Controller
@RequestMapping("/kubstuTest/questions")
@RequiredArgsConstructor
public class QuestionUploadController {

    private final QuestionService questionService;
    private final TeacherRepository teacherRepository;
    private final Teacher_SubjectRepository teacherSubjectRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;

    // GET /kubstuTest/questions/upload-form?subjectId=...
    @GetMapping("/upload-form")
    public String showUploadForm(
            @RequestParam(required = false) Integer subjectId,
            Model model,
            Principal principal) {

        Teacher teacher = getCurrentTeacher();

        List<Subject> subjects = teacherSubjectRepository.findByTeacherId(teacher.getId()).stream()
                .map(Teacher_Subject::getSubject)
                .toList();

        model.addAttribute("subjects", subjects);

        if (subjectId != null) {
            List<Topic> topics = topicRepository.findBySubjectId(subjectId);
            model.addAttribute("topics", topics);
            model.addAttribute("selectedSubjectId", subjectId);
        } else if (!subjects.isEmpty()) {
            Subject firstSubject = subjects.get(0);
            List<Topic> topics = topicRepository.findBySubjectId(firstSubject.getId());
            model.addAttribute("topics", topics);
            model.addAttribute("selectedSubjectId", firstSubject.getId());
        } else {
            model.addAttribute("topics", Collections.emptyList());
        }

        return "upload-questions";
    }

    // POST /kubstuTest/questions/upload
    @PostMapping("/upload")
    public String uploadQuestions(
            @RequestParam("topicId") Integer topicId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttrs,
            Principal principal) {

        if (file.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Файл не выбран или повреждён");
            return "redirect:/kubstuTest/questions/upload-form";
        }

        try {
            questionService.processQuestionFile(topicId, file);

            Topic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));

            Integer subjectId = topic.getSubject().getId();
            redirectAttrs.addFlashAttribute("success", "✅ Вопросы успешно загружены");
            return "redirect:/kubstuTest/questions/upload-form?subjectId=" + subjectId;

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "❌ Ошибка при обработке файла: " + e.getMessage());
            return "redirect:/kubstuTest/questions/upload-form";
        }
    }

    // GET /kubstuTest/questions/subjects-by-teacher
    @ResponseBody
    @GetMapping("/subjects-by-teacher")
    public List<Subject> getSubjectsByTeacher(Principal principal) {
        Teacher teacher = getCurrentTeacher();
        return teacherSubjectRepository.findByTeacherId(teacher.getId()).stream()
                .map(Teacher_Subject::getSubject)
                .toList();
    }

    // GET /kubstuTest/questions/topics-by-subject?subjectId=...
    @ResponseBody
    @GetMapping("/topics-by-subject")
    public List<Topic> getTopicsBySubject(@RequestParam Integer subjectId) {
        return topicRepository.findBySubjectId(subjectId);
    }

    // Получаем текущего учителя из SecurityContext
    private Teacher getCurrentTeacher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Пользователь не авторизован");
        }

        String username;
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = authentication.getName();
        }

        return teacherRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("Учитель не найден: " + username));
    }
}