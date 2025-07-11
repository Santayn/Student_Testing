package org.santayn.testing.service;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LectureService {

    private final LectureRepository lectureRepository;
    private final SubjectRepository subjectRepository;
    private final UserSearch userSearch;

    public LectureService(LectureRepository lectureRepository, SubjectRepository subjectRepository, UserSearch userSearch) {
        this.lectureRepository = lectureRepository;
        this.subjectRepository = subjectRepository;
        this.userSearch = userSearch;
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
        // Находим предмет по ID
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        // Получаем текущего учителя
        Teacher currentTeacher = userSearch.getCurrentTeacher();

        // Проверяем, что предмет принадлежит учителю
        boolean isSubjectBelongsToTeacher = currentTeacher.getTeacherSubjects().stream()
                .anyMatch(ts -> ts.getSubject(  ).getId().equals(subjectId));

        if (!isSubjectBelongsToTeacher) {
            throw new RuntimeException("Teacher does not have access to this subject");
        }

        // Создаем новую лекцию
        Lecture lecture = new Lecture();
        lecture.setSubject(subject);
        lecture.setTitle(title);
        lecture.setContent(content);
        lecture.setDescription(description);

        // Сохраняем лекцию
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

    public List<Lecture> getLectureByID(Integer subjectId) {
        if (subjectId == null) {
            throw new IllegalArgumentException("Test ID cannot be null");
        }
        return lectureRepository.findLectureBySubjectId(subjectId);
    }
    public List<Lecture> getLecturesBySubject(Integer subjectId) {
        return lectureRepository.findLectureBySubjectId(subjectId);
    }
}