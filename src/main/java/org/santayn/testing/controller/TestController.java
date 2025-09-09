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
import org.santayn.testing.models.answer.AnswerResult;
import org.santayn.testing.models.student.Student;
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

    // ▼ добавлено для группированного отчёта
    private final AnswerResultRepository answerResultRepository;
    private final StudentRepository studentRepository;

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
            TestGroupRepository testGroupRepository,
            AnswerResultRepository answerResultRepository,
            StudentRepository studentRepository) {
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
        this.answerResultRepository = answerResultRepository;
        this.studentRepository = studentRepository;
    }

    // ---------- СТАРЫЕ МЕТОДЫ СОЗДАНИЯ ТЕСТА (как у вас) ----------

    @GetMapping("/create-test")
    public String showCreateTestForm(
            @RequestParam(required = false) Integer selectedSubjectId,
            @RequestParam(required = false) List<Integer> selectedGroupIds,
            @RequestParam(required = false) Boolean success,
            Model model,
            Principal principal) {

        Teacher teacher = getCurrentTeacher();

        List<Subject> subjects = teacher_subjectRepository.findByTeacherId(teacher.getId()).stream()
                .map(Teacher_Subject::getSubject)
                .toList();

        List<Group> groups = teacher_groupRepository.findByTeacherId(teacher.getId()).stream()
                .map(Teacher_Group::getGroup)
                .toList();

        Integer subjectId = selectedSubjectId != null
                ? selectedSubjectId
                : (subjects.isEmpty() ? null : subjects.get(0).getId());

        List<Topic> topics = (subjectId != null)
                ? topicRepository.findBySubjectId(subjectId)
                : Collections.emptyList();

        List<Lecture> lectures = (subjectId != null)
                ? lectureRepository.findLectureBySubjectId(subjectId)
                : Collections.emptyList();

        if (selectedGroupIds == null) {
            selectedGroupIds = Collections.emptyList();
        }

        model.addAttribute("subjects", subjects);
        model.addAttribute("groups", groups);
        model.addAttribute("topics", topics);
        model.addAttribute("lectures", lectures);
        model.addAttribute("selectedSubjectId", subjectId);
        model.addAttribute("selectedGroupIds", selectedGroupIds);
        model.addAttribute("success", Boolean.TRUE.equals(success));

        return "create-test";
    }

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

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Лекция не найдена"));

        Test test = new Test();
        test.setTopic(topic);
        test.setName(name);
        test.setDescription(description);
        test.setQuestionCount(questionCount);
        test = testService.save(test);

        Test_Lecture testLecture = new Test_Lecture();
        testLecture.setTest(test);
        testLecture.setLecture(lecture);
        testLectureService.save(testLecture);

        if (selectedGroupIds != null && !selectedGroupIds.isEmpty()) {
            List<Group> groups = groupRepository.findAllById(selectedGroupIds);
            for (Group group : groups) {
                Test_Group testGroup = new Test_Group();
                testGroup.setTest(test);
                testGroup.setGroup(group);
                testGroupRepository.save(testGroup);
            }
        }

        StringBuilder redirectUrl = new StringBuilder("/kubstuTest/tests/create-test");
        if (subjectId != null) {
            redirectUrl.append("?selectedSubjectId=").append(subjectId);
        } else {
            redirectUrl.append("?selectedSubjectId=");
        }

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

    private Teacher getCurrentTeacher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Пользователь не авторизован");
        }
        String username = (authentication.getPrincipal() instanceof UserDetails ud)
                ? ud.getUsername()
                : authentication.getName();

        return teacherRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден: " + username));
    }

    // ---------- Вспомогательные API (как у вас) ----------

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

    // ==================== НОВОЕ: группированный просмотр результатов ====================

    /**
     * URL: /kubstuTest/tests/{testId}/results
     * Отображает для преподавателя:
     *  - Название группы
     *    - Фамилия студента
     *      - его ответы + краткая статистика
     */
    @GetMapping("/{testId}/results")
    public String viewResultsGrouped(@PathVariable Integer testId, Model model) {
        Teacher teacher = getCurrentTeacher();

        // группы, к которым у преподавателя есть доступ
        Set<Integer> allowedGroupIds = teacher_groupRepository.findByTeacherId(teacher.getId()).stream()
                .map(tg -> tg.getGroup().getId())
                .collect(Collectors.toSet());

        // группы, к которым прикреплён тест (пересечение с allowedGroupIds)
        List<Test_Group> links = testGroupRepository.findByTestId(testId);
        List<Group> groups = links.stream()
                .map(Test_Group::getGroup)
                .filter(g -> allowedGroupIds.contains(g.getId()))
                .toList();

        // grouped: Group -> List<Map: student, results, right, total, percent>
        Map<Group, List<Map<String, Object>>> grouped = new LinkedHashMap<>();

        for (Group g : groups) {
            List<Student> students = studentRepository.findByGroup_Id(g.getId());

            // sort by lastName if possible, fallback to Student.getName()
            students = students.stream()
                    .sorted(Comparator.comparing(s ->
                            Optional.ofNullable(s.getUser())
                                    .map(u -> u.getLastName() == null ? "" : u.getLastName())
                                    .orElse(s.getName() == null ? "" : s.getName())
                    ))
                    .toList();

            List<Map<String, Object>> perGroup = new ArrayList<>();

            for (Student s : students) {
                List<AnswerResult> answers =
                        answerResultRepository.findByGroupStudentAndTest(g.getId(), s.getId(), testId);

                int total = answers.size();
                int right = (int) answers.stream().filter(AnswerResult::isCorrect).count();
                int percent = total == 0 ? 0 : (int) Math.round(right * 100.0 / total);

                Map<String, Object> row = new HashMap<>();
                row.put("student", s);
                row.put("results", answers);
                row.put("right", right);
                row.put("total", total);
                row.put("percent", percent);

                perGroup.add(row);
            }

            grouped.put(g, perGroup);
        }

        model.addAttribute("grouped", grouped);
        model.addAttribute("groupsOrder", grouped.keySet().stream().toList());
        model.addAttribute("testId", testId);

        return "test-result";
    }
}
