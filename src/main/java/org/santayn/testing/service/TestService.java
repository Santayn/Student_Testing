package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.Question_In_Test;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.Test_Group;
import org.santayn.testing.models.test.Test_Lecture;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final QuestionInTestRepository questionInTestRepository;

    // для привязок
    private final LectureRepository lectureRepository;
    private final Test_LectureRepository testLectureRepository;
    private final GroupRepository groupRepository;
    private final TestGroupRepository testGroupRepository;

    /* ================== БАЗА ================== */

    public Test save(Test test) {
        return testRepository.save(test);
    }

    public Test getTestById(Integer testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест с ID " + testId + " не найден"));
    }

    /**
     * Возвращает случайные вопросы для теста, учитывая желаемый questionCount.
     * Верхнего лимита нет — берём min(доступно, questionCount).
     */
    public List<Question> generateQuestionsForTest(Integer testId) {
        Test test = getTestById(testId);
        List<Question> questions = questionRepository.findByTopic(test.getTopic());
        Collections.shuffle(questions);
        int limit = Math.min(questions.size(), Math.max(1, test.getQuestionCount()));
        return questions.stream().limit(limit).toList();
    }

    /* ================== СОЗДАНИЕ И ПРИВЯЗКИ ================== */

    /**
     * Создаёт тест, привязывает лекцию и группы (обязательно),
     * а затем наполняет тест случайными вопросами по теме.
     * - questionCount без верхнего лимита (>=1).
     * - Если вопросов меньше, чем просили — добавим сколько есть (не падаем).
     */
    @Transactional
    public Test createAndPopulateTest(Topic topic,
                                      Integer lectureId,
                                      Collection<Integer> groupIds,
                                      String name,
                                      String description,
                                      int questionCount) {

        if (groupIds == null || groupIds.isEmpty()) {
            throw new IllegalArgumentException("Нужно выбрать хотя бы одну группу");
        }
        if (lectureId == null) {
            throw new IllegalArgumentException("lectureId обязателен");
        }
        if (questionCount < 1) {
            throw new IllegalArgumentException("questionCount должен быть >= 1");
        }

        // 1) создать тест
        Test test = new Test();
        test.setTopic(topic);
        test.setName(name);
        test.setDescription(description);
        test.setQuestionCount(questionCount);
        test = testRepository.save(test);

        // 2) привязать лекцию
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Лекция не найдена: " + lectureId));
        Test_Lecture tl = new Test_Lecture();
        tl.setTest(test);
        tl.setLecture(lecture);
        testLectureRepository.save(tl);

        // 3) привязать группы (обязательно)
        attachGroups(test, groupIds);

        // 4) наполнить вопросами по теме
        populateWithRandomQuestions(test);

        return test;
    }

    /**
     * Привязать к тесту группы (без дубликатов).
     */
    @Transactional
    public void attachGroups(Test test, Collection<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) return;

        // уникализируем
        Set<Integer> uniqueIds = new LinkedHashSet<>(groupIds);
        List<Group> groups = groupRepository.findAllById(uniqueIds);

        // проверим, что все существуют
        if (groups.size() != uniqueIds.size()) {
            Set<Integer> foundIds = groups.stream().map(Group::getId).collect(Collectors.toSet());
            List<Integer> missing = uniqueIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new IllegalArgumentException("Не найдены группы: " + missing);
        }

        for (Group g : groups) {
            Test_Group tg = new Test_Group();
            tg.setTest(test);
            tg.setGroup(g);
            testGroupRepository.save(tg);
        }
    }

    /**
     * Привязать к тесту одну лекцию (заменить существующую связь на новую).
     */
    @Transactional
    public void attachOrReplaceLecture(Test test, Integer lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Лекция не найдена: " + lectureId));

        // Если у вас допускается несколько лекций на тест — уберите очистку.
        // Здесь пример замены на одну лекцию:
        List<Test_Lecture> existing = testLectureRepository.findByTest_Id(test.getId());
        for (Test_Lecture tl : existing) {
            testLectureRepository.delete(tl);
        }

        Test_Lecture tl = new Test_Lecture();
        tl.setTest(test);
        tl.setLecture(lecture);
        testLectureRepository.save(tl);
    }

    /* ================== ВОПРОСЫ В ТЕСТ ================== */

    /**
     * Наполнить тест случайными вопросами по теме теста.
     * Берём min(доступно, questionCount). Предыдущие связи не удаляем.
     */
    @Transactional
    public void populateWithRandomQuestions(Test test) {
        List<Question> pool = questionRepository.findByTopic(test.getTopic());
        if (pool.isEmpty()) return;

        Collections.shuffle(pool);
        int toTake = Math.min(pool.size(), Math.max(1, test.getQuestionCount()));
        List<Question> selected = pool.subList(0, toTake);

        addQuestionsToTest(test, selected);
    }

    /**
     * Добавить конкретные вопросы к тесту (без проверки дубликатов на уровне БД).
     */
    @Transactional
    public void addQuestionsToTest(Test test, Collection<Question> questions) {
        for (Question q : questions) {
            Question_In_Test qit = new Question_In_Test();
            qit.setTest(test);
            qit.setQuestion(q);
            questionInTestRepository.save(qit);
        }
    }

    /* ================== СТАРЫЕ МЕТОДЫ (обновлены под новые правила) ================== */

    /**
     * Старый API: создать тест и добавить в него случайные вопросы.
     * Больше не падаем при нехватке вопросов — просто берём сколько есть.
     */
    @Transactional
    public Test createTestWithRandomQuestions(Topic topic, int questionCount) {
        if (questionCount < 1) {
            throw new IllegalArgumentException("questionCount должен быть >= 1");
        }

        List<Question> allQuestions = questionRepository.findByTopic(topic);
        Collections.shuffle(allQuestions);

        int toTake = Math.min(allQuestions.size(), questionCount);
        List<Question> selectedQuestions = allQuestions.subList(0, toTake);

        Test test = new Test();
        test.setTopic(topic);
        test.setQuestionCount(questionCount);
        test.setDescription("Автосгенерированный тест");
        test = testRepository.save(test);

        addQuestionsToTest(test, selectedQuestions);

        return test;
    }
}
