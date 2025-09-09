package org.santayn.testing.service;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LectureService {

    private final LectureRepository lectureRepository;
    private final SubjectRepository subjectRepository;
    private final TestRepository    testRepository;
    private final StudentService    studentService;
    private final UserSearch        userSearch;

    public LectureService(LectureRepository lectureRepository,
                          SubjectRepository subjectRepository,
                          TestRepository testRepository,
                          StudentService studentService,
                          UserSearch userSearch) {
        this.lectureRepository = lectureRepository;
        this.subjectRepository = subjectRepository;
        this.testRepository    = testRepository;
        this.studentService    = studentService;
        this.userSearch        = userSearch;
    }

    // ------------------ ЛЕКЦИИ ------------------

    /** Получить все лекции по ID предмета */
    public List<Lecture> getLecturesBySubjectId(Integer subjectId) {
        if (subjectId == null) throw new IllegalArgumentException("Subject ID cannot be null");
        return lectureRepository.findLectureBySubjectId(subjectId);
    }

    /** Получить конкретную лекцию по ID предмета и ID лекции */
    public Lecture getSpecificLectureBySubjectIdAndLectureId(Integer subjectId, Integer lectureId) {
        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        return lectureRepository.findById(lectureId)
                .filter(lecture -> lecture.getSubject().getId().equals(subjectId))
                .orElseThrow(() -> new RuntimeException(
                        "Lecture with ID " + lectureId + " not found for Subject with ID " + subjectId));
    }

    /** Найти лекцию по ID (удобно для контроллера) */
    public Lecture findById(Integer lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + lectureId));
    }

    /** Добавить новую лекцию для предмета (проверяем, что предмет принадлежит текущему преподавателю) */
    public Lecture addLecture(Integer subjectId, String title, String content, String description) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        Teacher currentTeacher = userSearch.getCurrentTeacher();
        boolean isSubjectBelongsToTeacher = currentTeacher.getTeacherSubjects().stream()
                .anyMatch(ts -> ts.getSubject().getId().equals(subjectId));
        if (!isSubjectBelongsToTeacher) {
            throw new RuntimeException("Teacher does not have access to this subject");
        }

        Lecture lecture = new Lecture();
        lecture.setSubject(subject);
        lecture.setTitle(title);
        lecture.setContent(content);
        lecture.setDescription(description);
        return lectureRepository.save(lecture);
    }

    /** Удалить лекцию по ID */
    public void deleteLecture(Integer lectureId) {
        if (!lectureRepository.existsById(lectureId)) {
            throw new RuntimeException("Lecture with ID " + lectureId + " not found");
        }
        lectureRepository.deleteById(lectureId);
    }

    /** Синонимы/обёртки, если где-то используются */
    public List<Lecture> getLectureByID(Integer subjectId) { // историческое название
        return getLecturesBySubjectId(subjectId);
    }
    public List<Lecture> getLecturesBySubject(Integer subjectId) {
        return getLecturesBySubjectId(subjectId);
    }

    // ------------------ ТЕСТЫ ДЛЯ ЛЕКЦИИ ------------------

    /** Все тесты, прикреплённые к лекции (без фильтрации по группам) — для преподавателя/админа */
    public List<Test> getTestsForLecture(Integer lectureId) {
        if (lectureId == null) throw new IllegalArgumentException("Lecture ID cannot be null");
        findById(lectureId); // убедимся, что лекция существует
        return testRepository.findByLecture(lectureId);
    }

    /** Тесты лекции, доступные КОНКРЕТНОМУ студенту (фильтрация по его группе) */
    public List<Test> getTestsForLectureAndStudent(Integer lectureId, Integer studentId) {
        if (lectureId == null) throw new IllegalArgumentException("Lecture ID cannot be null");
        if (studentId == null) throw new IllegalArgumentException("Student ID cannot be null");

        findById(lectureId); // убедимся, что лекция существует

        // ⬇⬇⬇ ИСПРАВЛЕНО: распаковываем Optional<Student>
        Student student = studentService.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: id=" + studentId));

        Integer groupId = resolveStudentGroupId(student);
        if (groupId == null) {
            return List.of(); // студент без группы — тестов нет
        }
        return testRepository.findByLectureAndGroup(lectureId, groupId);
    }

    /** Вспомогательно: определяем группу студента (primary или первую из many-to-many) */
    private Integer resolveStudentGroupId(Student student) {
        if (student == null) return null;
        if (student.getGroup() != null) {
            Group g = student.getGroup();
            return g != null ? g.getId() : null;
        }
        if (student.getGroupStudents() != null && !student.getGroupStudents().isEmpty()) {
            Group_Student gs = student.getGroupStudents().get(0);
            Group g = gs.getGroup();
            return g != null ? g.getId() : null;
        }
        return null;
    }
}
