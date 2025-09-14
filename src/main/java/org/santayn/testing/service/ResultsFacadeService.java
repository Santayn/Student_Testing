package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.answer.AnswerResult;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResultsFacadeService {

    private final SubjectRepository subjectRepository;
    private final LectureRepository lectureRepository;
    private final Test_LectureRepository testLectureRepository;
    private final TestGroupRepository testGroupRepository;
    private final Teacher_GroupRepository teacherGroupRepository;
    private final StudentRepository studentRepository;
    private final AnswerResultRepository answerResultRepository;

    // Добавлены репозитории для получения имён теста и группы
    private final TestRepository testRepository;
    private final GroupRepository groupRepository;

    /* ====== КАСКАДНЫЕ СПИСКИ ====== */

    public List<Subject> allSubjects() {
        return subjectRepository.findAllSubjects();
    }

    public List<Lecture> lecturesBySubject(Integer subjectId) {
        if (subjectId == null) return Collections.emptyList();
        return lectureRepository.findLectureBySubjectId(subjectId);
    }

    public List<Test> testsByLecture(Integer lectureId) {
        if (lectureId == null) return Collections.emptyList();
        return testLectureRepository.findTestsByLectureId(lectureId);
    }

    public List<Group> groupsByTest(Integer testId, String teacherLogin) {
        if (testId == null) return Collections.emptyList();

        List<Group> groupsForTest = Optional.ofNullable(
                testGroupRepository.findGroupsByTestId(testId)
        ).orElseGet(Collections::emptyList);

        List<Group> allowedForTeacher = Optional.ofNullable(
                teacherGroupRepository.findGroupsByTeacherLogin(teacherLogin)
        ).orElseGet(Collections::emptyList);

        if (allowedForTeacher.isEmpty() || groupsForTest.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> allowedIds = allowedForTeacher.stream()
                .map(Group::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return groupsForTest.stream()
                .filter(g -> g.getId() != null && allowedIds.contains(g.getId()))
                .toList();
    }

    public List<Student> studentsByGroup(Integer groupId) {
        if (groupId == null) return Collections.emptyList();
        return Optional.ofNullable(studentRepository.findByGroup_Id(groupId))
                .orElseGet(Collections::emptyList);
    }

    /* ====== РЕЗУЛЬТАТЫ И СТАТИСТИКА ====== */

    /**
     * Грузим результаты с безопасной деградацией:
     * - если есть testId — берём все ответы по тесту и дальше режем по группе/студенту в памяти;
     * - иначе, если есть studentId — берём его ответы и опционально режем по группе (на случай если студент не из группы);
     * - иначе, если есть только groupId — собираем студентов группы и конкатенируем их ответы;
     * - иначе — пусто.
     */
    public List<AnswerResult> loadResults(Integer testId, Integer groupId, Integer studentId) {

        // Множество studentId, допустимых по выбранной группе (если группа выбрана)
        Set<Integer> groupStudentIds = Collections.emptySet();
        if (groupId != null) {
            groupStudentIds = studentsByGroup(groupId).stream()
                    .map(Student::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (groupStudentIds.isEmpty()) {
                // Если в группе никого — сразу возвращаем пусто, чтобы не бить БД лишний раз
                return List.of();
            }
        }

        List<AnswerResult> base;

        if (testId != null) {
            // Базово — все по тесту
            base = Optional.ofNullable(answerResultRepository.findByTestId(testId))
                    .orElseGet(Collections::emptyList);
        } else if (studentId != null) {
            // Базово — все по студенту
            base = Optional.ofNullable(answerResultRepository.findByStudentId(studentId))
                    .orElseGet(Collections::emptyList);
        } else if (groupId != null) {
            // Только группа: соберём по каждому студенту группы (да, несколько запросов, зато без новых методов репозитория)
            List<AnswerResult> acc = new ArrayList<>();
            for (Integer sid : groupStudentIds) {
                acc.addAll(Optional.ofNullable(answerResultRepository.findByStudentId(sid))
                        .orElseGet(Collections::emptyList));
            }
            base = acc;
        } else {
            return List.of();
        }

        // Фильтрация по студенту (если задан)
        if (studentId != null) {
            base = base.stream()
                    .filter(ar -> Objects.equals(ar.getStudentId(), studentId))
                    .toList();
        }

        // Фильтрация по группе (если задана)
        if (groupId != null) {
            final Set<Integer> allowed = groupStudentIds; // effectively final для лямбды
            base = base.stream()
                    .filter(ar -> ar.getStudentId() != null && allowed.contains(ar.getStudentId()))
                    .toList();
        }

        // Фильтрация по тесту (если базой был studentId/groupId без testId)
        if (testId != null) {
            base = base.stream()
                    .filter(ar -> Objects.equals(ar.getTestId(), testId))
                    .toList();
        }

        return base;
    }

    public int countTotal(List<AnswerResult> results) {
        return results == null ? 0 : results.size();
    }

    public int countRight(List<AnswerResult> results) {
        if (results == null) return 0;
        return (int) results.stream().filter(AnswerResult::isCorrect).count();
    }

    public int percent(int right, int total) {
        return total > 0 ? Math.round(right * 100f / total) : 0;
    }

    /** Имя студента по id */
    public String studentName(Integer studentId) {
        if (studentId == null) return null;
        return studentRepository.findById(studentId).map(Student::getName).orElse(null);
    }

    /** Имя теста по id */
    public String testName(Integer testId) {
        if (testId == null) return null;
        return testRepository.findById(testId).map(Test::getName).orElse(null);
    }

    /** Имя группы по id */
    public String groupName(Integer groupId) {
        if (groupId == null) return null;
        return groupRepository.findById(groupId).map(Group::getName).orElse(null);
    }

    /** Мапа studentId -> имя (для вывода в таблице) */
    public Map<Integer, String> studentNamesMap(List<AnswerResult> results) {
        if (results == null || results.isEmpty()) return Map.of();

        // Собираем уникальные id студентов из результатов
        Set<Integer> ids = results.stream()
                .map(AnswerResult::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (ids.isEmpty()) return Map.of();

        // Тащим студентов пачкой
        List<Student> students = studentRepository.findAllById(ids);

        Map<Integer, String> map = new HashMap<>();
        for (Student s : students) {
            if (s.getId() != null) {
                map.put(s.getId(), s.getName());
            }
        }

        // Для отсутствующих — поставим «—», чтобы в шаблоне не падать
        ids.forEach(id -> map.putIfAbsent(id, "—"));

        return map;
    }
}
