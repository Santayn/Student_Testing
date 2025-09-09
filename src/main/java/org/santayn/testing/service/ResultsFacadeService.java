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

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultsFacadeService {

    private final SubjectRepository subjectRepository;
    private final LectureRepository lectureRepository;
    private final Test_LectureRepository testLectureRepository;
    private final TestGroupRepository testGroupRepository;
    private final Teacher_GroupRepository teacherGroupRepository;
    private final StudentRepository studentRepository;
    private final AnswerResultRepository answerResultRepository;

    /* ====== КАСКАДНЫЕ СПИСКИ ====== */

    /** Все предметы */
    public List<Subject> allSubjects() {
        return subjectRepository.findAllSubjects();
    }

    /** Лекции выбранного предмета */
    public List<Lecture> lecturesBySubject(Integer subjectId) {
        if (subjectId == null) return Collections.emptyList();
        return lectureRepository.findLectureBySubjectId(subjectId);
    }

    /** Тесты выбранной лекции */
    public List<Test> testsByLecture(Integer lectureId) {
        if (lectureId == null) return Collections.emptyList();
        return testLectureRepository.findTestsByLectureId(lectureId);
    }

    /** Группы по тесту */
    public List<Group> groupsByTest(Integer testId, String teacherLogin) {
        if (testId == null) return Collections.emptyList();
        List<Group> groups = testGroupRepository.findGroupsByTestId(testId);
        // ограничим группами, к которым привязан преподаватель
        List<Group> allowed = teacherGroupRepository.findGroupsByTeacherLogin(teacherLogin);
        if (allowed == null || allowed.isEmpty()) return Collections.emptyList();
        return groups.stream()
                .filter(g -> allowed.stream().anyMatch(a -> a.getId().equals(g.getId())))
                .toList();
    }

    /** Студенты группы */
    public List<Student> studentsByGroup(Integer groupId) {
        if (groupId == null) return Collections.emptyList();
        return studentRepository.findByGroup_Id(groupId);
    }

    /* ====== РЕЗУЛЬТАТЫ И СТАТИСТИКА ====== */

    public List<AnswerResult> loadResults(Integer testId, Integer groupId, Integer studentId) {
        if (testId != null && groupId != null && studentId != null) {
            return answerResultRepository.findByGroupStudentAndTest(groupId, studentId, testId);
        }
        if (groupId != null && studentId != null) {
            return answerResultRepository.findByGroupIdAndStudentId(groupId, studentId);
        }
        if (groupId != null) {
            return answerResultRepository.findByGroupId(groupId);
        }
        if (studentId != null) {
            return answerResultRepository.findByStudentId(studentId);
        }
        if (testId != null) {
            return answerResultRepository.findByTestId(testId);
        }
        return List.of();
    }

    public int countTotal(List<AnswerResult> results) {
        return results == null ? 0 : results.size();
    }

    public int countRight(List<AnswerResult> results) {
        if (results == null) return 0;
        return (int) results.stream()
                .filter(AnswerResult::isCorrect) // у тебя должно быть поле boolean correct
                .count();
    }

    public int percent(int right, int total) {
        return total > 0 ? Math.round(right * 100f / total) : 0;
    }

    public String studentName(Integer studentId) {
        if (studentId == null) return null;
        return studentRepository.findById(studentId)
                .map(Student::getName)
                .orElse(null);
    }
}
