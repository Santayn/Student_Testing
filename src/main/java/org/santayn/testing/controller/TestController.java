package org.santayn.testing.controller;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.Test_Lecture;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher_Subject;
import org.santayn.testing.models.test.Test_Group;
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
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/kubstuTest/tests")
public class TestController {

    private final TestService testService;
    private final TopicRepository topicRepository;
    private final LectureRepository lectureRepository;
    private final SubjectRepository subjectRepository;
    private final TestLectureService testLectureService;
    private final Teacher_SubjectRepository teacher_subjectRepository;
    private final Teacher_GroupRepository teacher_groupRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final TestGroupRepository testGroupRepository;

    public TestController(
            TestService testService,
            TopicRepository topicRepository,
            LectureRepository lectureRepository,
            SubjectRepository subjectRepository,
            TestLectureService testLectureService,
            Teacher_SubjectRepository teacher_subjectRepository,
            Teacher_GroupRepository teacher_groupRepository,
            TeacherRepository teacherRepository,
            GroupRepository groupRepository,
            TestGroupRepository testGroupRepository) {
        this.testService = testService;
        this.topicRepository = topicRepository;
        this.lectureRepository = lectureRepository;
        this.subjectRepository = subjectRepository;
        this.testLectureService = testLectureService;
        this.teacher_subjectRepository = teacher_subjectRepository;
        this.teacher_groupRepository = teacher_groupRepository;
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.testGroupRepository = testGroupRepository;
    }

    /**
     * Отображение формы создания теста.
     * Поддерживает параметры: selectedSubjectId, selectedGroupIds, success.
     */
    @GetMapping("/create-test")
    public String showCreateTestForm(
            @RequestParam(required = false) Integer selectedSubjectId,
            @RequestParam(required = false) List<Integer> selectedGroupIds,
            @RequestParam(required = false) Boolean success,
            Model model,
            Principal principal) {

        Teacher teacher = getCurrentTeacher();

        // Получаем предметы и группы, доступные учителю
        List<Subject> subjects = teacher_subjectRepository.findByTeacherId(teacher.getId()).stream()
                .map(Teacher_Subject::getSubject)
                .toList();

        List<Group> groups = teacher_groupRepository.findByTeacherId(teacher.getId()).stream()
                .map(Teacher_Group::getGroup)
                .toList();

        // Определяем активный предмет
        Integer subjectId = selectedSubjectId != null
                ? selectedSubjectId
                : (subjects.isEmpty() ? null : subjects.get(0).getId());

        // Загружаем темы и лекции по предмету
        List<Topic> topics = (subjectId != null)
                ? topicRepository.findBySubjectId(subjectId)
                : Collections.emptyList();

        List<Lecture> lectures = (subjectId != null)
                ? lectureRepository.findLectureBySubjectId(subjectId)
                : Collections.emptyList();

        // Гарантируем, что selectedGroupIds не null
        if (selectedGroupIds == null) {
            selectedGroupIds = Collections.emptyList();
        }

        // Передаём данные в модель
        model.addAttribute("subjects", subjects);
        model.addAttribute("groups", groups);
        model.addAttribute("topics", topics);
        model.addAttribute("lectures", lectures);
        model.addAttribute("selectedSubjectId", subjectId);
        model.addAttribute("selectedGroupIds", selectedGroupIds);
        model.addAttribute("success", Boolean.TRUE.equals(success)); // true, если ?success=true

        return "create-test";
    }

    /**
     * Обработка создания теста.
     */
    @PostMapping("/create-test")
    public String createTest(
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) List<Integer> selectedGroupIds,
            @RequestParam Integer topicId,
            @RequestParam Integer lectureId,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam int questionCount,
            Model model,
            Principal principal) {

        // 1. Получаем связанные сущности
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Лекция не найдена"));

        // 2. Создаём и сохраняем тест
        Test test = new Test();
        test.setTopic(topic);
        test.setName(name);
        test.setDescription(description);
        test.setQuestionCount(questionCount);
        test = testService.save(test);

        // 3. Связываем тест с лекцией
        Test_Lecture testLecture = new Test_Lecture();
        testLecture.setTest(test);
        testLecture.setLecture(lecture);
        testLectureService.save(testLecture);

        // 4. Связываем тест с группами (если выбраны)
        if (selectedGroupIds != null && !selectedGroupIds.isEmpty()) {
            List<Group> groups = groupRepository.findAllById(selectedGroupIds);
            for (Group group : groups) {
                Test_Group testGroup = new Test_Group();
                testGroup.setTest(test);
                testGroup.setGroup(group);
                testGroupRepository.save(testGroup);
            }
        }

        // 5. Редирект на форму с параметрами
        StringBuilder redirectUrl = new StringBuilder("/kubstuTest/tests/create-test");
        if (subjectId != null) {
            redirectUrl.append("?selectedSubjectId=").append(subjectId);
        } else {
            redirectUrl.append("?selectedSubjectId=");
        }

        // Добавляем выбранные группы
        if (selectedGroupIds != null && !selectedGroupIds.isEmpty()) {
            redirectUrl.append("&selectedGroupIds=").append(
                    selectedGroupIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","))
            );
        }

        redirectUrl.append("&success=true");

        return "redirect:" + redirectUrl;
    }

    // ✅ Получить текущего учителя
    private Teacher getCurrentTeacher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Пользователь не авторизован");
        }

        String username = authentication.getPrincipal() instanceof UserDetails userDetails
                ? userDetails.getUsername()
                : authentication.getName();

        return teacherRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден: " + username));
    }

    // 🔽 API-эндпоинты

    @GetMapping("/subjects-by-teacher")
    @ResponseBody
    public List<Subject> getSubjectsByTeacher(Principal principal) {
        Teacher teacher = getCurrentTeacher();
        return teacher_subjectRepository.findByTeacherId(teacher.getId()).stream()
                .map(Teacher_Subject::getSubject)
                .toList();
    }

    @GetMapping("/groups-by-teacher")
    @ResponseBody
    public List<Group> getGroupsByTeacher(Principal principal) {
        Teacher teacher = getCurrentTeacher();
        return teacher_groupRepository.findByTeacherId(teacher.getId()).stream()
                .map(Teacher_Group::getGroup)
                .toList();
    }

    @GetMapping("/topics-by-subject")
    @ResponseBody
    public List<Topic> getTopicsBySubject(@RequestParam Integer subjectId) {
        return topicRepository.findBySubjectId(subjectId);
    }

    @GetMapping("/lectures-by-subject")
    @ResponseBody
    public List<Lecture> getLecturesBySubject(@RequestParam Integer subjectId) {
        List<Lecture> lectures = lectureRepository.findLectureBySubjectId(subjectId);
        System.out.println("Найдено лекций: " + lectures.size());
        lectures.forEach(l -> System.out.println("Лекция: " + l.getId() + " - " + l.getTitle()));
        return lectures;
    }
}