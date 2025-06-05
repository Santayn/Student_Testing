package org.santayn.testing.controller;

import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.Test_Lecture;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher_Subject;
import org.santayn.testing.repository.*;
import org.santayn.testing.service.TestLectureService;
import org.santayn.testing.service.TestService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/kubstuTest/tests")
public class TestController {

    private final TestService testService;
    private final TopicRepository topicRepository;
    private final LectureRepository lectureRepository;
    private final SubjectRepository subjectRepository;
    private final TestLectureService testLectureService;
    private final Teacher_SubjectRepository teacher_subjectRepository;
    private final TeacherRepository teacherRepository; // Добавили зависимость

    public TestController(
            TestService testService,
            TopicRepository topicRepository,
            LectureRepository lectureRepository,
            SubjectRepository subjectRepository,
            TestLectureService testLectureService,
            Teacher_SubjectRepository teacher_subjectRepository,
            TeacherRepository teacherRepository) { // Инжектируем TeacherRepository
        this.testService = testService;
        this.topicRepository = topicRepository;
        this.lectureRepository = lectureRepository;
        this.subjectRepository = subjectRepository;
        this.testLectureService = testLectureService;
        this.teacher_subjectRepository = teacher_subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    // 🟢 Форма создания теста
    @GetMapping("/create-test")
    public String showCreateTestForm(Model model, Principal principal) {
        Teacher currentTeacher = getCurrentTeacher(); // Теперь используем новый метод

        List<Subject> subjects = teacher_subjectRepository.findByTeacherId(currentTeacher.getId()).stream()
                .map(Teacher_Subject::getSubject)
                .toList();

        model.addAttribute("subjects", subjects);
        return "create-test";
    }

    // 🟢 Логика сохранения теста
    @PostMapping("/create-test")
    public String createTest(@RequestParam Integer topicId,
                             @RequestParam Integer lectureId,
                             @RequestParam int questionCount,
                             Model model) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Тема не найдена"));

        Test test = new Test();
        test.setTopic(topic);
        test.setQuestionCount(questionCount);

        test = testService.save(test);

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Лекция не найдена"));

        Test_Lecture testLecture = new Test_Lecture();
        testLecture.setLecture(lecture);
        testLecture.setTest(test);

        testLectureService.save(testLecture);

        return "redirect:/kubstuTest/lecture/" + lectureId;
    }

    // ✅ Получить текущего учителя из SecurityContext
    private Teacher getCurrentTeacher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        String username;
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = authentication.getName();
        }

        return teacherRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("Teacher not found for user: " + username));
    }
}