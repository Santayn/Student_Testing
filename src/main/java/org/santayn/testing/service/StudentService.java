
package org.santayn.testing.service;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.StudentRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Optional<Student> findById(Integer id) {
        return studentRepository.findById(id);
    }
    // Получение списка студентов по ID группы
    public List<Student> getStudentsByGroupID(Integer groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }
        System.out.println("Fetching students for group ID: " + groupId); // Логирование
        List<Student> students = studentRepository.findStudentByGroupId(groupId);
        System.out.println("Students found: " + students.size()); // Проверка количества найденных студентов
        return students;
    }

    // Получение конкретного студента по ID группы и ID студента
    public Student getSpecificStudentByGroupIdAndStudentId(Integer groupId, Integer studentId) {
        if (groupId == null || studentId == null) {
            throw new IllegalArgumentException("Group ID and Student ID cannot be null");
        }

        // Ищем студента напрямую через репозиторий
        return studentRepository.findStudentByGroupId(groupId).stream()
                .filter(student -> student.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Студент с ID " + studentId + " не найден в группе с ID " + groupId));
    }
    public List<Student> findFreeStudents(Integer groupId) {
        List<Student> allStudents = findAll();
        List<Student> studentsInGroup = getStudentsByGroupID(groupId);

        return allStudents.stream()
                .filter(student -> !studentsInGroup.contains(student))
                .toList();
    }
}