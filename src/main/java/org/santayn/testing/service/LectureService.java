package org.santayn.testing.service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.teacher.Teacher_Subject;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class LectureService {

    private final LectureRepository lectureRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public LectureService(LectureRepository lectureRepository, SubjectRepository subjectRepository, TeacherRepository teacherRepository) {
        this.lectureRepository = lectureRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    /**
     * Получить все лекции по ID предмета
     */
    public List<Lecture> getLecturesBySubjectId(Integer subjectId) {
        if (subjectId == null) {
            throw new IllegalArgumentException("Subject ID cannot be null");
        }
        return lectureRepository.findLectureBySubjectId(subjectId);
    }

    /**
     * Получить конкретную лекцию по ID предмета и ID лекции
     */
    public Lecture getSpecificLectureBySubjectIdAndLectureId(Integer subjectId, Integer lectureId) {
        // Проверяем, что предмет существует
        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        // Ищем лекцию по ID
        return lectureRepository.findById(lectureId)
                .filter(lecture -> lecture.getSubject().getId().equals(subjectId))
                .orElseThrow(() -> new RuntimeException(
                        "Lecture with ID " + lectureId + " not found for Subject with ID " + subjectId));
    }

    /**
     * Добавить новую лекцию для предмета
     */
    public Lecture addLecture(Integer subjectId, String title, String content, String description) {
        // 1. Находим предмет по ID
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        // 2. Получаем текущего учителя
        Teacher currentTeacher = getCurrentTeacher();

        // 3. Проверяем, что предмет принадлежит учителю
        boolean isSubjectBelongsToTeacher = currentTeacher.getTeacherSubjects().stream()
                .anyMatch(ts -> ts.getSubject().getId().equals(subjectId));

        if (!isSubjectBelongsToTeacher) {
            throw new RuntimeException("Teacher does not have access to this subject");
        }

        // 4. Создаем новую лекцию
        Lecture lecture = new Lecture();
        lecture.setSubject(subject);
        lecture.setTitle(title);
        lecture.setContent(content);
        lecture.setDescription(description);

        // 5. Сохраняем лекцию
        return lectureRepository.save(lecture);
    }

    /**
     * Удалить лекцию по ID
     */
    public void deleteLecture(Integer lectureId) {
        // Проверяем, что лекция существует
        if (!lectureRepository.existsById(lectureId)) {
            throw new RuntimeException("Lecture with ID " + lectureId + " not found");
        }
        lectureRepository.deleteById(lectureId);
    }

    /**
     * Получить текущего учителя
     */
    private Teacher getCurrentTeacher() {
        // Получаем объект Authentication из SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Проверяем, что пользователь аутентифицирован
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        // Получаем имя пользователя (логин)
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();

        // Ищем учителя по логину в базе данных
        return teacherRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("Teacher not found for user: " + username));
    }
}