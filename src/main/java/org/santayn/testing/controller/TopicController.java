package org.santayn.testing.controller;

import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.TopicService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class TopicController {

    private final TopicService topicService;
    private final SubjectService subjectService;

    public TopicController(TopicService topicService, SubjectService subjectService) {
        this.topicService = topicService;
        this.subjectService = subjectService;
    }

    /**
     * Показать страницу управления темами для предмета
     */
    @GetMapping("/manage-topics-subject")
    public String showManageTopicsPage(
            @RequestParam(required = false) Integer subjectId,
            Model model) {

        // Получаем текущего учителя
        Teacher currentTeacher = topicService.getCurrentTeacher();

        // Получаем все предметы, которые принадлежат текущему учителю
        List<Subject> teacherSubjects = subjectService.getSubjectsByTeacher(currentTeacher);
        model.addAttribute("subjects", teacherSubjects);

        if (subjectId != null) {
            // Проверяем, что выбранный предмет принадлежит текущему учителю
            boolean isSubjectBelongsToTeacher = teacherSubjects.stream()
                    .anyMatch(subject -> subject.getId().equals(subjectId));

            if (!isSubjectBelongsToTeacher) {
                throw new RuntimeException("Учитель не имеет доступа к этому предмету");
            }

            // Получаем темы для выбранного предмета
            List<Topic> topics = topicService.getTopicsBySubjectId(subjectId);
            model.addAttribute("subjectId", subjectId);
            model.addAttribute("topics", topics);
        }

        return "manage-topics-subject"; // Шаблон: manage-topics-subject.html
    }

    /**
     * Добавить новую тему к предмету
     */
    @PostMapping("/add-topic-to-subject")
    public String addTopicToSubject(
            @RequestParam Integer subjectId,
            @RequestParam String name) {

        topicService.addTopic(subjectId, name);
        return "redirect:/kubstuTest/manage-topics-subject?subjectId=" + subjectId;
    }

    /**
     * Удалить тему из предмета
     */
    @PostMapping("/remove-topic-from-subject")
    public String removeTopicFromSubject(
            @RequestParam Integer subjectId,
            @RequestParam Integer topicId) {

        topicService.deleteTopic(topicId);
        return "redirect:/kubstuTest/manage-topics-subject?subjectId=" + subjectId;
    }

    /**
     * Получить список тем по subjectId в формате JSON (REST)
     */
    @GetMapping("/topics/by-subject/{subjectId}")
    public ResponseEntity<List<Topic>> getTopicsBySubject(@PathVariable Integer subjectId) {
        List<Topic> topics = topicService.getTopicsBySubjectId(subjectId);
        return ResponseEntity.ok(topics);
    }
}